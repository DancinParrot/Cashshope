package sg.edu.np.mad_p03_group_gg.view.ui;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.payments.paymentlauncher.PaymentLauncher;
import com.stripe.android.payments.paymentlauncher.PaymentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.edu.np.mad_p03_group_gg.R;
import sg.edu.np.mad_p03_group_gg.models.DeliveryAddress;
import sg.edu.np.mad_p03_group_gg.tools.FirebaseTools;
import sg.edu.np.mad_p03_group_gg.tools.ImageDownloader;
import sg.edu.np.mad_p03_group_gg.tools.StripeUtils;

public class CheckoutActivity extends AppCompatActivity {
    private static FirebaseDatabase database = FirebaseDatabase.getInstance("https://cashoppe-179d4-default-rtdb.asia-southeast1.firebasedatabase.app/");
    private static DatabaseReference databaseReference = database.getReference();
    private static final String BACKEND_URL = "https://cashshope.japaneast.cloudapp.azure.com/";
    private String paymentIntentClientSecret;
    private PaymentLauncher paymentLauncher;
    private OkHttpClient httpClient = new OkHttpClient();
    private PaymentMethodCreateParams params;
    private String paymentMethod;
    private FirebaseAuth auth;
    private String userId;
    private String stripeAccountId;
    private Boolean isAddressValid = false;
    private String json;
    private int totalPrice = 0;
    private final StripeDialog stripeDialog = new StripeDialog(CheckoutActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        ImageView closeButton = findViewById(R.id.paymentMethodCloseButton);
        TextView listingTitleView = findViewById(R.id.listingTitle);
        TextView listingPriceView = findViewById(R.id.listingPrice);
        ImageView listingPictureView = findViewById(R.id.listingPictureView);
        TextView listingTitlePriceCard = findViewById(R.id.listingTitlePriceCard);
        TextView listingCostPriceCard = findViewById(R.id.listingCostPriceCard);
        TextView deliveryCostTextView = findViewById(R.id.deliveryCostTextView);
        TextView totalPriceTextView = findViewById(R.id.totalPriceTextView);
        ImageView paymentMethodLogo = findViewById(R.id.paymentMethodLogo);
        TextView paymentDetailsHint = findViewById(R.id.paymentDetailsHint);
        TextView changePaymentButton = findViewById(R.id.changePaymentButton);
        LinearLayout addressLayout = findViewById(R.id.addressLayout);
        Button checkoutButton = findViewById(R.id.checkoutButton);

        RadioGroup deliveryRadioGroup = findViewById(R.id.deliveryRadioGroup);
        RadioButton deliveryRadioButton = findViewById(R.id.deliveryRadioButton);
        RadioButton meetupRadioButton = findViewById(R.id.meetupRadioButton);

        // Get Intent from Individual Listing Activity
        stripeAccountId = getIntent().getStringExtra("stripeAccountId");
        String productId = getIntent().getStringExtra("productId");

        // Get current user id
        auth = FirebaseAuth.getInstance();
        FirebaseUser fbUser = auth.getCurrentUser();
        userId = fbUser.getUid();

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        PaymentConfiguration.init(
                getApplicationContext(),
                "pk_test_51LKF7ZFaaAQicG0TEdtmijoaa2muufF73f7Hyhid3hXglesPpgV86ykgKWxJ74zwkrzbWa7HvrAvZExbVD5wDV1X0017hZyVPa",
                stripeAccountId
        );

        // Start checkout session (create paymentIntent to get clientSecret)
        // PaymentIntent is provided the seller's Stripe Account ID to enable direct payments
        paymentLauncher = PaymentLauncher.Companion.create(
                this,
                PaymentConfiguration.getInstance(this).getPublishableKey(),
                PaymentConfiguration.getInstance(this).getStripeAccountId(),
                this::onPaymentResult
        );


        // Retrive listing details from Firebase given the productId
        databaseReference.child("users").child(userId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    FirebaseTools.createListingObjectFromFirebase(productId,
                            CheckoutActivity.this, listingObject -> {
                                // Interface is used to ensure data is retrieved fully before
                                // using it to set data variables due to it being asynchronous
                                listingTitleView.setText(listingObject.getTitle());
                                listingPriceView.setText("$" + listingObject.getPrice());

                                new ImageDownloader(listingPictureView).execute(listingObject.gettURLs().get(0));
                                listingTitlePriceCard.setText(listingObject.getTitle());
                                listingCostPriceCard.setText("$" + listingObject.getPrice());
                                if (listingObject.getDeliveryPrice().equals("")) {
                                    deliveryCostTextView.setText("$0");
                                    totalPrice = Integer.parseInt(listingObject.getPrice()) + 0;
                                } else {
                                    deliveryCostTextView.setText("$" + listingObject.getDeliveryPrice());
                                    totalPrice = Integer.parseInt(listingObject.getPrice()) +
                                            Integer.parseInt(listingObject.getDeliveryPrice());
                                }
                                totalPriceTextView.setText("$" + totalPrice);

                                // If seller did not enable delivery, set view to GONE
                                if (listingObject.getDelivery() == false) {
                                    deliveryRadioButton.setVisibility(View.GONE);
                                    addressLayout.setVisibility(View.GONE);
                                    meetupRadioButton.setChecked(true); // check the meetup option
                                }

                                String customerEmail = task.getResult().child("email").getValue(String.class);

                                checkoutButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        // Provide customer detail to Stripe to send receipt
                                        startCheckout(totalPrice, customerEmail, stripeAccountId);
                                    }
                                });
                            });
                }
            }
        });

        // Get result from PaymentMethod Activity (card info)
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        paymentMethod = data.getStringExtra("paymentMethod");

                        if (paymentMethod.equals("Card"))
                        {
                            params = data.getParcelableExtra("cardInfo");
                            // Set last 4 digits of card (allow user to double check)
                            paymentDetailsHint.setText("***" + params.getCard().getLast4());
                        }
                    }
                });

        changePaymentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent paymentMethodIntent = new Intent(CheckoutActivity.this,
                        PaymentMethodActivity.class);
                activityResultLauncher.launch(paymentMethodIntent);
            }
        });

        // Set visibility of addressLayout (delivery address .etc) according to the selection
        // of radio buttons. (If meetup is selected, don't let user enter delivery address)
        deliveryRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int selectedPaymentMethodId = deliveryRadioGroup.getCheckedRadioButtonId();
                RadioButton selectedRadioButton = findViewById(selectedPaymentMethodId);

                if (selectedPaymentMethodId != -1)
                {
                    if (selectedRadioButton.getText().equals("Meetup"))
                    {
                        addressLayout.setVisibility(View.GONE);
                    }
                    else
                    {
                        addressLayout.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        // Active Input Validation
        // Delivery Address
        EditText addressLine1 = findViewById(R.id.editAddressLine1);
        EditText addressLine2 = findViewById(R.id.editAddressLine2);
        EditText postalCode = findViewById(R.id.editPostalCode);
        EditText shippingName = findViewById(R.id.editName);

        addressLine1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (TextUtils.isEmpty(addressLine1.getText().toString())) {
                    addressLine1.setError("A valid Singapore address is required,");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        addressLine2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (TextUtils.isEmpty(addressLine2.getText().toString())) {
                    addressLine2.setError("A valid Singapore address is required,");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        postalCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (TextUtils.isEmpty(postalCode.getText().toString())) {
                    postalCode.setError("A valid Singapore postal code is required,");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        shippingName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (TextUtils.isEmpty(shippingName.getText().toString())) {
                    shippingName.setError("A valid Singapore postal code is required,");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    /**
     * Only for Stripe Payment
     *
     * 1) Gets email address of current authenticated user
     * 2) Send POST request to backend to create payment intent
     * 3) Get client secret from payment intent (NOT the whole payment intent) to confirm payment.
     *
     * @param totalPrice
     * @param customerEmail
     * @param stripeAccountId
     */
    private void startCheckout(int totalPrice, String customerEmail, String stripeAccountId){

        // Validate if Delivery Address entered is valid
        if (paymentMethod != null) {
            isAddressValid = validateDeliveryAddress();

            if (isAddressValid)
            {
                // Disable button after confirm payment, to prevent duplicated charges
                Button checkoutButton = findViewById(R.id.checkoutButton);
                checkoutButton.setEnabled(false);
                RadioGroup deliveryRadioGroup = findViewById(R.id.deliveryRadioGroup);
                int selectedRadioButtonId = deliveryRadioGroup.getCheckedRadioButtonId();
                RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);

                // If meetup is selected the payment intent will not contain a shipping address
                if (selectedRadioButton.getText().toString().equals("Meetup"))
                {
                    // Stripe uses smallest currency unit, therefore need to multiply by 100
                    json = String.format("{"
                                    + "\"amount\":\"%s\","
                                    + "\"cust_email\":\"%s\","
                                    + "\"stripe_account\":\"%s\","
                                    + "\"shipping\":{"
                                    +   "\"address\":{"
                                    +       "\"city\":\"null\","
                                    +       "\"country\":\"null\","
                                    +       "\"line1\":\"null\","
                                    +       "\"line2\":\"null\","
                                    +       "\"postal_code\":\"null\","
                                    +       "\"state\":\"null\""
                                    +   "},"
                                    +   "\"name\":\"null\""
                                    + "}"
                                    + "}", String.valueOf(totalPrice * 100), customerEmail,
                            stripeAccountId);

                }
                else
                {
                    // Customer's delivery address is sent to Stripe, allowing Seller to directly
                    // manage the orders from the dashboard.

                    EditText addresLine1 = findViewById(R.id.editAddressLine1);
                    EditText addressLine2 = findViewById(R.id.editAddressLine2);
                    EditText postalCode = findViewById(R.id.editPostalCode);
                    EditText shippingName = findViewById(R.id.editName);

                    DeliveryAddress deliveryAddress = new DeliveryAddress(
                            addresLine1.getText().toString(),
                            addressLine2.getText().toString(),
                            postalCode.getText().toString(),
                            shippingName.getText().toString()
                    );

                    json = String.format("{"
                                    + "\"amount\":\"%s\","
                                    + "\"cust_email\":\"%s\","
                                    + "\"stripe_account\":\"%s\","
                                    + "\"shipping\":{"
                                    +   "\"address\":{"
                                    +       "\"city\":\"Singapore\","
                                    +       "\"country\":\"SG\","
                                    +       "\"line1\":\"%s\","
                                    +       "\"line2\":\"%s\","
                                    +       "\"postal_code\":\"%s\","
                                    +       "\"state\":\"Singapore\""
                                    +   "},"
                                    +   "\"name\":\"%s\""
                                    + "}"
                                    + "}", String.valueOf(totalPrice * 100), customerEmail,
                            stripeAccountId, deliveryAddress.getLine1(), deliveryAddress.getLine2(),
                            String.valueOf(deliveryAddress.getPostalCode()),
                            deliveryAddress.getShippingName());

                }
                createPaymentIntent(); //
            }
            else
            {
                Toast.makeText(CheckoutActivity.this,
                        "Please select a delivery method.",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(CheckoutActivity.this,
                    "Please key in your payment information again.",
                    Toast.LENGTH_SHORT).show();
        }


    }

    private void onPaymentResult(PaymentResult paymentResult) {
        stripeDialog.dismissDialog();
        String message = "";
        Boolean isSuccess = false;

        if (paymentResult instanceof PaymentResult.Completed) {
            message = "Completed!";
            isSuccess = true;
        } else if (paymentResult instanceof PaymentResult.Canceled) {
            message = "Canceled!";
        } else if (paymentResult instanceof PaymentResult.Failed) {
            message = "Failed: " + ((PaymentResult.Failed) paymentResult).getThrowable().getMessage();
        }
        displayAlert("PaymentResult: ", message, isSuccess);
    }

    private void displayAlert(@NonNull String title, @Nullable String message, Boolean isSuccess) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if (isSuccess)
                {

                    String textMessage = "Hi, a payment has been made, please confirm.";

                    //FirebaseTools.sendConfirmationMessage(userId,  sellerId, textMessage); // inform seller of a payment
                    CheckoutActivity.this.finish();
                }
            }
        });

        builder.create().show();
    }

    /**
     * Executed when checkoutButton is pressed, to perform a final input validation on the
     * required information
     *
     * @return
     */
    private Boolean validateDeliveryAddress() {

        RadioGroup deliveryRadioGroup = findViewById(R.id.deliveryRadioGroup);
        int selectedRadioButtonId = deliveryRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);

        // If got at least one button selected
        if (deliveryRadioGroup.getCheckedRadioButtonId() != -1) {
            // Check if it's meetup or delivery
            if (selectedRadioButton.getText().toString().equals("Meetup")) {
                return true;
            } else {
                // Delivery Address
                EditText addresLine1 = findViewById(R.id.editAddressLine1);
                EditText addressLine2 = findViewById(R.id.editAddressLine2);
                EditText postalCode = findViewById(R.id.editPostalCode);
                EditText shippingName = findViewById(R.id.editName);

                if (TextUtils.isEmpty(addresLine1.getText().toString())
                        || TextUtils.isEmpty(addressLine2.getText().toString())
                        || TextUtils.isEmpty(postalCode.getText().toString())
                        || TextUtils.isEmpty(shippingName.getText().toString())) {
                    Toast.makeText(CheckoutActivity.this,
                            "Please enter the required information",
                            Toast.LENGTH_SHORT).show();
                    return false;

                } else if (selectedRadioButton.getText().toString().equals("Delivery")) {
                    return true;
                }
                else {
                    return false;
                }

            }
        }
        else {
            Toast.makeText(CheckoutActivity.this, "Please select one delivery method.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void createPaymentIntent()
    {
        // Create a PaymentIntent by calling the sample server's /create-payment-intent endpoint.
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        Log.d("json", json);
        RequestBody body = RequestBody.create(mediaType, json);
        Request request = new Request.Builder()
                .url(BACKEND_URL + "create-payment-intent")
                .post(body)
                .build();

        httpClient.newCall(request)
                .enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // Request failed
                        Log.d("Error", e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body().string();
                        try {
                            JSONObject responseJson = new JSONObject(body);
                            // Client secret is confidential and of one time use only, to confirm
                            // the payment by customers
                            paymentIntentClientSecret = responseJson.getString("client_secret");

                            // As this function is not in the main thread (which creates the UI
                            // elements), need to explicitly specify UI tasks such as dialogues
                            // to run on the main (UI) thread.
                            CheckoutActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    StripeUtils.confirmPayment(params, stripeDialog,
                                            paymentIntentClientSecret,
                                            paymentLauncher, CheckoutActivity.this);
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}