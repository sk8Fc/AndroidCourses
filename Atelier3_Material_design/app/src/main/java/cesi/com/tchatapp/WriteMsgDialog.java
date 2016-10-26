package cesi.com.tchatapp;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import cesi.com.tchatapp.utils.Constants;

/**
 * Created by sca on 04/06/15.
 */
public class WriteMsgDialog extends DialogFragment {

    private EditText message;
    String token;

    static WriteMsgDialog getInstance(final String token) {
        WriteMsgDialog f = new WriteMsgDialog();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("token", token);
        f.setArguments(args);

        return f;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        Bundle args = this.getArguments();
        token = args.getString("token");

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.dialog_msg, null);
        message = (EditText) view.findViewById(R.id.tchat_msg);

        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.validate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        closeKeyboard();
                        if (!message.getText().toString().isEmpty()) {
                            //post message
                            new SendMessageAsyncTask(view.getContext()).execute(message.getText().toString());
                        } else {
                            message.setError(WriteMsgDialog.this.getActivity()
                                    .getString(R.string.error_missing_msg));
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        closeKeyboard();
                        WriteMsgDialog.this.dismiss();
                    }
                }

        );

        return builder.create();
    }

    private void closeKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(message.getWindowToken(), 0);
    }

    /**
     * AsyncTask for sign-in
     */
    protected class SendMessageAsyncTask extends AsyncTask<String, Void, Integer> {

        Context context;

        public SendMessageAsyncTask(final Context context) {
            this.context = context;
        }

        @Override
        protected Integer doInBackground(String... params) {
            InputStream inputStream = null;
            try {
                //then create an httpClient.
                URL url = new URL(context.getString(R.string.url_msg));
                Log.d("Calling URL", url.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);

                //set authorization header
                conn.setRequestProperty("Authorization", "Bearer " + token);
                String urlParameters = "message="+params[0];

                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                return conn.getResponseCode();
            } catch (Exception e) {
                Log.d(Constants.TAG, "Error occured in your AsyncTask : ", e);
                return 500;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e("NetworkHelper", e.getMessage());
                    }
                }
            }
        }

        @Override
        public void onPostExecute(Integer status) {
            if (status != 201) {
                Toast.makeText(context, context.getString(R.string.error_send_msg), Toast.LENGTH_SHORT).show();
            }else {
                WriteMsgDialog.this.dismiss();
            }
        }
    }
}
