package android.infuzion.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChatActivity extends AppCompatActivity {
    Thread clientThread;
    Client client;
    String ip;
    int port;
    String username;
    Button button;
    EditText editText;
    private LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        linearLayout = (LinearLayout) findViewById(R.id.linLayout);
        linearLayout.addView(new TextView(getApplicationContext()));
        button = (Button) findViewById(R.id.sendButton);
        editText = (EditText) findViewById(R.id.messageText);

        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        ip = intent.getStringExtra("ip");
        port = intent.getIntExtra("port", 7776);
        username = intent.getStringExtra("username");

        client = new Client(ip, port, username);
        clientThread = new Thread(client);
        clientThread.start();
        client.scheduleTask(new Runnable() {
            @Override
            public void run() {
                client.sendMessage("123");
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.sendMessage(editText.getText().toString());
            }
        });

        client.setMessageListener(new ChatMessageListener() {
            @Override
            public void run(final String string) {
                Handler handler = new Handler(getApplicationContext().getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        addMessage(string);
                        editText.setText("");
                    }
                });
            }
        });
    }

    public void addMessage(String string) {
        TextView textView = new TextView(getBaseContext());
        textView.setText(string);
        linearLayout.addView(textView);
    }

}
