package org.wordpress.android.netservices;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.models.Account;
import org.wordpress.android.ui.accounts.helpers.LoginWPCom;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.VolleyUtils;
import org.xmlrpc.android.XMLRPCClient;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class SiteService {
    // Events class
    public static class SiteListReceived {
        public final List<Site> mSiteList;
        public SiteListReceived(List<Site> siteList) {
            mSiteList = siteList;
        }
    }
    public static class SiteListErrorReceived {
        public final int mMessageId;
        public SiteListErrorReceived(int messageId) {
            mMessageId = messageId;
        }
    }

    // TODO: move this
    public class Site {
        String mSiteName;
        String mUrl;
        int mSiteId;
        boolean mIsAdmin;
        boolean mIsVisible;
        String mXMLRPCUrl;
    }

    // Clients
    public RestClient mRESTComClient;
    public RestClient mRESTOrgClient;
    public XMLRPCClient mXMLRPCClient;

    //region Public methods

    /**
     * Get a list of the account's sites:
     *
     * Broadcast SiteListReceived on success or SiteListErrorReceived on error
     */
    public void getSiteList(Account account) {
        // If wpcom or JetPack+API enabled site
        if (account.isWordPressComUser()) {
            // Make a wpcom REST API call
            getSiteListRESTCom(account);
        } else {
            if (account.isWPAPISupported()) {
                // if the site supports it, make a wp-api REST API call
                // TODO: mRESTOrgClient.call
            } else {
                // Else make a XMLRPC call
                // TODO: mXMLRPCClient.call
            }
        }
    }
    //endregion

    //region private methods
    private void getSiteListRESTCom(Account account) {
        // TODO: setup mRESTComClient using account

        mRESTComClient.get("me/sites", new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                List<Site> siteList = convertJSONObjectToSiteList(response);
                EventBus.getDefault().post(new SiteListReceived(siteList));
                // TODO: save sites to DB but don't do it in the current thread
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                JSONObject errorObject = VolleyUtils.volleyErrorToJSON(volleyError);
                EventBus.getDefault().post(new SiteListErrorReceived(LoginWPCom.restLoginErrorToMsgId(errorObject)));
            }
        });
    }

    private Site convertJSONObjectToSite(JSONObject jsonObject) {
        Site site = new Site();
        try {
            site.mSiteName = jsonObject.getString("name");
            site.mUrl = jsonObject.getString("URL");
            site.mSiteId = jsonObject.getInt("ID");
            site.mIsAdmin = jsonObject.getBoolean("user_can_manage");
            site.mIsVisible = jsonObject.getBoolean("visible");
            JSONObject jsonLinks = JSONUtils.getJSONChild(jsonObject, "meta/links");
            if (jsonLinks != null) {
                site.mXMLRPCUrl = jsonLinks.getString("xmlrpc");
                return site;
            } else {
                AppLog.e(T.NUX, "xmlrpc links missing from the me/sites REST response");
            }
        } catch (JSONException e) {
            AppLog.e(T.NUX, e);
        }
        return null;
    }

    private List<Site> convertJSONObjectToSiteList(JSONObject jsonObject) {
        List<Site> sites = new ArrayList<>();
        JSONArray jsonSites = jsonObject.optJSONArray("sites");
        if (jsonSites != null) {
            for (int i = 0; i < jsonSites.length(); i++) {
                JSONObject jsonSite = jsonSites.optJSONObject(i);
                Site site = convertJSONObjectToSite(jsonSite);
                if (site != null) {
                    sites.add(site);
                }
            }
        }
        return sites;
    }
    //endregion
}
