package com.example.abhay.address

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.example.abhay.address.display.BaseActivity
import com.example.abhay.address.network.Address
import com.example.abhay.address.network.ErrorReply
import com.example.abhay.address.network.RetrofitClient
import com.google.gson.Gson
import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Pattern

/**
 * This class is used to add and update a particular address record.
 */
class AddOrEditAddressActivity : AppCompatActivity() {

    lateinit var requestObject: Address

    /**
     * will indicate whether the requested operation is an UPDATE query
     */
    var isUpdateQuery = false

    /**
     * will contain address ID in case it is an update query
     */
    var id = Int.MIN_VALUE

    lateinit var call: Call<JsonElement>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_or_edit_address)

        setImageButtonClickListener()
        if (intent.extras != null) {    // whether the intended query is an update request or not
            isUpdateQuery = true
            inializeForm()
            title = "Update Address"
        } else {
            //initializeFormTestingPurpose()  // for testing purpose
            title = "Add Address"
        }
    }

    /**
     * will register the click listener for the send button
     */
    private fun setImageButtonClickListener() {
        findViewById<ImageButton>(R.id.send_button).setOnClickListener {
            removeErrorFields()
            createRequestObject()
            call = if (isUpdateQuery) {
                RetrofitClient.client.updateAddress(id, requestObject)
            } else {
                RetrofitClient.client.createAddress(requestObject)
            }
            sendRequest()
        }
    }

    /**
     * This function will create and initialize the request object which is used to send data to the server.
     */
    private fun createRequestObject() {
        requestObject = Address().apply {
            val name = findViewById<EditText>(R.id.input_Name).text.toString().trim().split(Pattern.compile(" "), 2)
            firstname = name[0]
            lastname = if (name.size > 1) name[1] else null
            address1 = findViewById<EditText>(R.id.input_Address1).text.toString().trim()
            address2 = findViewById<EditText>(R.id.input_Address2).text.toString().trim() +
                    findViewById<EditText>(R.id.input_Landmark).text.toString().trim()
            city = findViewById<EditText>(R.id.input_City).text.toString().trim()
            stateId = findViewById<EditText>(R.id.input_State).text.toString().trim().takeIf { it.isNotEmpty() }?.toInt()
            zipcode = findViewById<EditText>(R.id.input_Zipcode).text.toString().trim()

            countryId = 105
            phone = (1234567890).toString()

        }
        Log.d("requestObject", requestObject.toString())
    }

    /**
     * This function will be send the post/put request depending upon the call object
     */
    private fun sendRequest() {

        call.enqueue(object : Callback<JsonElement> {

            override fun onResponse(call: Call<JsonElement>?, response: Response<JsonElement>?) {
                if (response?.code() == 200) {
                    val address = Gson().fromJson(response.body().toString(), Address::class.java)
                    startActivity(Intent(this@AddOrEditAddressActivity, BaseActivity::class.java).apply {
                        putExtra("address", address)
                        putExtra("isChecked", this@AddOrEditAddressActivity.findViewById<CheckBox>(R.id.checkBox_Make_Default_Address).isChecked)
                    })
                    this@AddOrEditAddressActivity.finish()
                } else if (response?.code() == 422) {
                    val error = Gson().fromJson(response.errorBody()?.string(), ErrorReply::class.java)
                    setErrorFields(error.errors)
                }
            }

            override fun onFailure(call: Call<JsonElement>?, t: Throwable?) {
                Toast.makeText(this@AddOrEditAddressActivity, "Error occurred", Toast.LENGTH_LONG).show()
            }

        })
    }

    /**
     * This will set the error fields for the input fields which has some error
     */
    private fun setErrorFields(errors: ErrorReply.Errors?) {
        if (errors?.city != null)
            findViewById<EditText>(R.id.input_City).error = errors.city?.get(0)
        if (errors?.address1 != null)
            findViewById<EditText>(R.id.input_Address1).error = errors.address1?.get(0)
        if (errors?.stateId != null)
            findViewById<EditText>(R.id.input_State).error = errors.stateId?.get(0)
        if (errors?.zipcode != null)
            findViewById<EditText>(R.id.input_Zipcode).error = errors.zipcode?.get(0)
    }

    /**
     * This will remove error fields from all the possibly erroneous fields before sending request to the server
     */
    private fun removeErrorFields() {
        findViewById<EditText>(R.id.input_City).error = null
        findViewById<EditText>(R.id.input_Address1).error = null
        findViewById<EditText>(R.id.input_State).error = null
        findViewById<EditText>(R.id.input_Zipcode).error = null
    }

    /**
     * Returns id of the default address
     */
    private fun getDefaultAddress(): Int {
        val sharedPreferences = getSharedPreferences("defaultAddress", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("id", Int.MIN_VALUE)
    }

    /**
     * This function is used to initialize the input fields if the requested operation is UPDATE record.
     */
    private fun inializeForm() {
        val address = (intent.extras["address"] as Bundle)["address"] as Address
        //Toast.makeText(this, address.toString(), Toast.LENGTH_LONG).show()
        findViewById<EditText>(R.id.input_Name).setText((address.firstname
                ?: "").plus(" ").plus((address.lastname ?: "")))
        findViewById<EditText>(R.id.input_Address1).setText(address.address1 ?: "")
        findViewById<EditText>(R.id.input_Address2).setText(address.address2 ?: "")
        findViewById<EditText>(R.id.input_City).setText(address.city ?: "")
        findViewById<EditText>(R.id.input_State).setText(address.stateId?.toString() ?: "")
        findViewById<EditText>(R.id.input_Zipcode).setText(address.zipcode ?: "")
        id = address.id!!
        if (id == getDefaultAddress()) {
            findViewById<CheckBox>(R.id.checkBox_Make_Default_Address).apply {
                isChecked = true
                isClickable = false
            }
        }
    }

    /**
     * This function was only created for testing.
     */
    private fun initializeFormTestingPurpose() {
        findViewById<EditText>(R.id.input_Name).setText("My name")
        findViewById<EditText>(R.id.input_Address1).setText("My address1")
        findViewById<EditText>(R.id.input_Address2).setText("My address2")
        findViewById<EditText>(R.id.input_City).setText("In my city")
        findViewById<EditText>(R.id.input_State).setText("1400")
        findViewById<EditText>(R.id.input_Zipcode).setText("284128")
    }
}