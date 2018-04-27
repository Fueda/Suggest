package jp.ac.titech.itpro.sdl.suggest;

import android.app.SearchManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.xmlpull.v1.XmlPullParser;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";

    private EditText queryInput;
    private ArrayAdapter<String> resultAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        queryInput = findViewById(R.id.query_input);
        Button suggestButton = findViewById(R.id.suggest_button);
        suggestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String queryText = queryInput.getText().toString().trim();
                if (queryText.length() > 0)
                    new SuggestTask(MainActivity.this).execute(queryText);
            }
        });
        ListView resultList = findViewById(R.id.result_list);
        resultAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new ArrayList<String>());
        resultList.setAdapter(resultAdapter);
        resultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                String text = (String)parent.getItemAtPosition(pos);
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.putExtra(SearchManager.QUERY, text);
                startActivity(intent);
            }
        });
    }

    public void showResult(List<String> result) {
        resultAdapter.clear();
        resultAdapter.addAll(result);
        resultAdapter.notifyDataSetChanged();
        queryInput.selectAll();
    }

    private static class SuggestTask extends AsyncTask<String, Void, List<String>> {

        private WeakReference<MainActivity> activityRef;
        private final String suggest_url;
        private final String result_no_suggestions;

        SuggestTask(MainActivity activity) {
            activityRef = new WeakReference<>(activity);
            suggest_url = activity.getResources().getString(R.string.suggest_url);
            result_no_suggestions = activity.getResources().getString(R.string.result_no_suggestions);
        }

        @Override
        protected List<String> doInBackground(String... strings) {
            List<String> result = new ArrayList<>();
            HttpURLConnection conn = null;
            String error = null;
            try {
                String query = URLEncoder.encode(strings[0], "UTF-8");
                URL url = new URL(suggest_url + query);
                conn = (HttpURLConnection)url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setDoInput(true);
                conn.connect();
                XmlPullParser xpp = Xml.newPullParser();
                xpp.setInput(conn.getInputStream(), "UTF-8");
                for (int et = xpp.getEventType();
                     et != XmlPullParser.END_DOCUMENT; et = xpp.next()) {
                    if (et == XmlPullParser.START_TAG &&
                            xpp.getName().equalsIgnoreCase("suggestion")) {
                        for (int i = 0; i < xpp.getAttributeCount(); i++)
                            if (xpp.getAttributeName(i).equalsIgnoreCase("data"))
                                result.add(xpp.getAttributeValue(i));
                    }
                }
            }
            catch (Exception e) {
                error = e.toString();
            }
            finally {
                if (conn != null) conn.disconnect();
            }
            if (error != null) {
                result.clear();
                result.add(error);
            }
            if (result.size() == 0)
                result.add(result_no_suggestions);
            return result;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            MainActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing())
                return;
            activity.showResult(result);
       }
    }
}
