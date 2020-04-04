package com.introid.chatapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.JsonReader;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.util.jar.JarEntry;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ChatActivity extends AppCompatActivity implements TextWatcher {

    private String name;
    private WebSocket webSocket;
    private String SERVER_PATH="http://ws//echo.websocket.org";
    private EditText messageEdit;
    private View sendBtn,pickImage;
    private RecyclerView recyclerView;
    private int IMAGE_REQUEST_ID=1;
    private MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_chat );

        name= getIntent().getStringExtra( "name" );
        initiateSocketConnection();
    }

    private void initiateSocketConnection() {
        OkHttpClient client= new OkHttpClient();
        Request request= new Request.Builder().url(SERVER_PATH).build();
        webSocket=client.newWebSocket( request,new SocketListener() );

    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        
    }

    @Override
    public void afterTextChanged(Editable editable) {
        String string= editable.toString().trim();
        if (string.isEmpty()){
            resetMessageEdit();
        }else{
            sendBtn.setVisibility( View.VISIBLE );
            pickImage.setVisibility( View.INVISIBLE );
        }
    }

    private void resetMessageEdit() {
        messageEdit.removeTextChangedListener( this );
        messageEdit.setText( "" );
        sendBtn.setVisibility( View.INVISIBLE );
        pickImage.setVisibility(View.VISIBLE );

        messageEdit.addTextChangedListener( this );
    }

    private class SocketListener extends WebSocketListener{
        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            super.onOpen( webSocket, response );
            runOnUiThread( () ->{
                Toast.makeText( ChatActivity.this, "Socket Connection Successful", Toast.LENGTH_SHORT ).show();
                initializeView();
            } );
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            super.onMessage( webSocket, text );

            runOnUiThread( () ->{
                try {
                    JSONObject jsonObject= new JSONObject();
                    jsonObject.put( "isSent",false );

                    messageAdapter.addItem( jsonObject );

                    recyclerView.smoothScrollToPosition(messageAdapter.getItemCount()-1  );

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } );
        }
    }

    private void initializeView() {
        messageEdit= findViewById(R.id.messageEdit);
        sendBtn= findViewById( R.id.btn_send );
        pickImage= findViewById( R.id.image );
        recyclerView= findViewById( R.id.recycler );

        messageAdapter = new MessageAdapter( getLayoutInflater() );
        recyclerView.setAdapter(messageAdapter);
        recyclerView.setLayoutManager( new LinearLayoutManager( this ) );

        messageEdit.addTextChangedListener( this );

        sendBtn.setOnClickListener( view -> {
            JSONObject jsonObject= new JSONObject();
            try {
                jsonObject.put( "name" ,name);
                jsonObject.put( "message",messageEdit.getText().toString().trim() );

                webSocket.send( jsonObject.toString() );

                jsonObject.put( "isSent",true );

                messageAdapter.addItem( jsonObject );

                recyclerView.smoothScrollToPosition( messageAdapter.getItemCount()-1 );

                resetMessageEdit();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } );
        pickImage.setOnClickListener( view -> {
            Intent intent= new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType( "image/*" );

            startActivityForResult( Intent.createChooser( intent,"Pick Image" ),IMAGE_REQUEST_ID );

        } );

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult( requestCode, resultCode, data );
        if (requestCode == IMAGE_REQUEST_ID && resultCode == RESULT_OK){
            try {
                InputStream is= getContentResolver().openInputStream( data.getData() );
                Bitmap image=BitmapFactory.decodeStream(is);
                
                sendImage(image);
                
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendImage(Bitmap image) {
        ByteArrayOutputStream outputStream= new ByteArrayOutputStream();
        image.compress( Bitmap.CompressFormat.JPEG,50,outputStream );

        String base64String = Base64.encodeToString( outputStream.toByteArray(),Base64.DEFAULT );

        Reader in;
        JSONObject jsonObject= new JSONObject();

        try {
            jsonObject.put( "name",name );
            jsonObject.put( "image",base64String );

            webSocket.send( jsonObject.toString() );

            jsonObject.put( "isSent",true );

            messageAdapter.addItem( jsonObject );
            recyclerView.smoothScrollToPosition( messageAdapter.getItemCount()-1 );

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
