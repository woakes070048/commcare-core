package org.commcare.util.screen;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.CommCareSession;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.QueryPrompt;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.core.util.OrderedHashtable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_SELECT1;

/**
 * Screen that displays user configurable entry texts and makes
 * a case query to the server with these fields.
 *
 * @author wspride
 */
public class QueryScreen extends Screen {

    private RemoteQuerySessionManager remoteQuerySessionManager;
    private OrderedHashtable<String, QueryPrompt> userInputDisplays;
    private SessionWrapper sessionWrapper;
    private String[] fields;
    private String mTitle;
    private String currentMessage;

    private String domainedUsername;
    private String password;

    private PrintStream out;

    public QueryScreen(String domainedUsername, String password, PrintStream out) {
        this.domainedUsername = domainedUsername;
        this.password = password;
        this.out = out;
    }

    @Override
    public void init(SessionWrapper sessionWrapper) throws CommCareSessionException {
        this.sessionWrapper = sessionWrapper;
        remoteQuerySessionManager =
                RemoteQuerySessionManager.buildQuerySessionManager(sessionWrapper,
                        sessionWrapper.getEvaluationContext());

        if (remoteQuerySessionManager == null) {
            throw new CommCareSessionException(String.format("QueryManager for case " +
                    "claim screen with id %s cannot be null.", sessionWrapper.getNeededData()));
        }
        userInputDisplays = remoteQuerySessionManager.getNeededUserInputDisplays();

        int count = 0;
        fields = new String[userInputDisplays.keySet().size()];
        for (Map.Entry<String, QueryPrompt> queryPromptEntry : userInputDisplays.entrySet()) {
            fields[count] = queryPromptEntry.getValue().getDisplay().getText().evaluate(sessionWrapper.getEvaluationContext());
        }

        try {
            mTitle = Localization.get("case.search.title");
        } catch (NoLocalizedTextException nlte) {
            mTitle = "Case Claim";
        }
    }

    private static String buildUrl(String baseUrl, Hashtable<String, String> queryParams) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
        for (String key : queryParams.keySet()) {
            urlBuilder.addQueryParameter(key, queryParams.get(key));
        }
        return urlBuilder.build().toString();
    }


    private InputStream makeQueryRequestReturnStream() {
        String url = buildUrl(getBaseUrl().toString(), getQueryParams());
        String credential = Credentials.basic(domainedUsername, password);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .build();
        try {
            Response response = new OkHttpClient().newCall(request).execute();
            return response.body().byteStream();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean processResponse(InputStream responseData) {
        if (responseData == null) {
            currentMessage = "Query result null.";
            return false;
        }
        Pair<ExternalDataInstance, String> instanceOrError =
                remoteQuerySessionManager.buildExternalDataInstance(responseData);
        if (instanceOrError.first == null) {
            currentMessage = "Query response format error: " + instanceOrError.second;
            return false;
        } else {
            sessionWrapper.setQueryDatum(instanceOrError.first);
            return true;
        }
    }

    private boolean isResponseEmpty(ExternalDataInstance instance) {
        return !instance.getRoot().hasChildren();
    }

    public void answerPrompts(Hashtable<String, String> answers) {
        for (String key : userInputDisplays.keySet()) {
            QueryPrompt queryPrompt = userInputDisplays.get(key);
            String answer = answers.get(key);

            // Treat all missing values as empty
            if (answer == null) {
                answer = "";
            }

            // If select question, we should have got an index as the answer which should
            // be converted to the corresponding value
            if (queryPrompt.isSelectOne() && !answer.isEmpty()) {
                int choiceIndex = Integer.parseInt(answer);
                Vector<SelectChoice> selectChoices = queryPrompt.getItemsetBinding().getChoices();
                answer = selectChoices.get(choiceIndex).getValue();
            }
            remoteQuerySessionManager.answerUserPrompt(key, answer);
        }
    }

    public void refreshItemSetChoices() {
        remoteQuerySessionManager.refreshItemSetChoices(remoteQuerySessionManager.getUserAnswers());
    }

    protected URL getBaseUrl() {
        return remoteQuerySessionManager.getBaseUrl();
    }

    protected Hashtable<String, String> getQueryParams() {
        return remoteQuerySessionManager.getRawQueryParams();
    }

    public String getScreenTitle() {
        return mTitle;
    }

    @Override
    public void prompt(PrintStream out) {
        out.println("Enter the search fields as a space separated list.");
        for (int i = 0; i < fields.length; i++) {
            out.println(i + ") " + fields[i]);
        }
    }

    @Override
    public String[] getOptions() {
        return fields;
    }

    @Override
    public boolean handleInputAndUpdateSession(CommCareSession session, String input) {
        String[] answers = input.split(",");
        Hashtable<String, String> userAnswers = new Hashtable<>();
        int count = 0;
        for (Map.Entry<String, QueryPrompt> queryPromptEntry : userInputDisplays.entrySet()) {
            userAnswers.put(queryPromptEntry.getKey(), answers[count]);
            count++;
        }
        answerPrompts(userAnswers);
        InputStream response = makeQueryRequestReturnStream();
        boolean refresh = processResponse(response);
        if (currentMessage != null) {
            out.println(currentMessage);
        }
        return refresh;
    }


    public OrderedHashtable<String, QueryPrompt> getUserInputDisplays() {
        return userInputDisplays;
    }

    public String getCurrentMessage() {
        return currentMessage;
    }

    public Hashtable<String, String> getCurrentAnswers() {
        return remoteQuerySessionManager.getUserAnswers();
    }
}
