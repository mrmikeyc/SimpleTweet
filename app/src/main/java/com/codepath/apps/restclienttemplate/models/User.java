package com.codepath.apps.restclienttemplate.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcel;

// Since the User model is contained within the Tweet Model, this also needs to be Parcelable
@Parcel
public class User {

    private static final String TAG = User.class.getSimpleName();

    public String name;
    public String screenName;
    public String profileImageURL;

    public User() {
    }

    public static User fromJson(JSONObject jsonObject) throws JSONException {
        User user = new User();

        user.name = jsonObject.getString("name");
        user.screenName = jsonObject.getString("screen_name");
        user.profileImageURL = jsonObject.getString("profile_image_url_https");

        return user;
    }
}
