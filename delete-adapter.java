package net.atos.ulive.views;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


import net.atos.soclomo.android.common.utils.JacksonUtils;
import net.atos.soclomo.android.common.utils.NetworkUtil;
import net.atos.ulive.R;
import net.atos.ulive.adapters.FeedListCustomAdapter;
import net.atos.ulive.constants.Constants;
import net.atos.ulive.dao.FeedLikeDAO;
import net.atos.ulive.dto.CriticalAnnouncement;
import net.atos.ulive.dto.DocumentRepositoryDTO;
import net.atos.ulive.dto.FeedCentral;
import net.atos.ulive.dto.FeedLikedDTO;
import net.atos.ulive.dto.MDsDesk;
import net.atos.ulive.dto.ParseDataNotFoundJsonResponse;
import net.atos.ulive.dto.ParseDocumentRepositoryResponseMethodName;
import net.atos.ulive.dto.ParseDocumentRepositoryResponseTable;
import net.atos.ulive.dto.ParseDocumentRepositoryResponseTag;
import net.atos.ulive.dto.ParseJsonErrorResponse;
import net.atos.ulive.dto.ParseUliveFeed;
import net.atos.ulive.dto.PureLife;
import net.atos.ulive.interfaces.UliveFeed;
import net.atos.ulive.services.UliveRestCall;
import net.atos.utils.BaseApplication;
import net.atos.utils.interfaces.BfsHosts;
import net.atos.utils.interfaces.CPSessionHeaders;
import net.atos.utils.interfaces.NetworkCallback;
import net.atos.utils.views.BaseActivity;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class UliveFeedListActivity extends BaseActivity implements
		OnItemClickListener, BfsHosts, CPSessionHeaders {

	private ListView mFeedList;
	private int mFeedType;
	private List<UliveFeed> mList = new ArrayList<UliveFeed>();;
	private TextView mTxtNoFeeds;
	private ProgressDialog mProgressDialog;
	private View activityView;

	FeedLikeDAO feedLikeDAO = null;
	private List<FeedLikedDTO> feedlikedlist = new ArrayList<FeedLikedDTO>();
	private BaseApplication baseApp = null;
	private int loggedInUserID;
	
	private List<DocumentRepositoryDTO> mDocumentList = null;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initViews();
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			return;
		}

		mFeedType = extras.getInt(Constants.FEED_TYPE);
		String requestUrl = "";
		String documentCategory = "";

		if (mFeedType == UliveFeed.FEED_TYPE_CRITICAL_ANNOUNCEMENT) {
			setTitle(Constants.CRITICAL_ANNOUNCEMENT_TITLE);
			requestUrl = Constants.ULIVE_CRITICAL_ANNOUNCEMENT_URL;
			documentCategory = Constants.CRITICAL_ANNOUNCEMENT_TITLE;

		} else if (mFeedType == UliveFeed.FEED_TYPE_PURE_LIFE) {
			setTitle(Constants.PURE_LIFE_TITLE);
			requestUrl = Constants.ULIVE_PURE_LIFE_URL;
			documentCategory = Constants.PURE_LIFE_TITLE;

		} else if (mFeedType == UliveFeed.FEED_TYPE_ORG_ANNOUNCEMENT) {
			setTitle(Constants.ORG_ANNOUNCEMENT_TITLE);
			requestUrl = Constants.ULIVE_ORG_ANNOUNCEMENT_URL;
			documentCategory = Constants.ORG_ANNOUNCEMENT_TITLE;

		} else if (mFeedType == UliveFeed.FEED_TYPE_ARCHIVAL) {
			setTitle(Constants.ARCHIVAL_TITLE);
			requestUrl = Constants.ULIVE_ARCHIVE;
			documentCategory = Constants.ARCHIVAL_TITLE;

		} else if (mFeedType == UliveFeed.FEED_TYPE_MD_DESK) {
			setTitle(Constants.MD_DESK_TITLE);
			requestUrl = Constants.ULIVE_MD_DESK_URL;
			documentCategory = Constants.MD_DESK_TITLE;
		}

		post(documentCategory);
		
		/** Fetched logged in users liked feeds for specific Feed Type */
		this.getLikedFeedsWithType();
		postUliveFeed(requestUrl, mFeedType);

	}

	/**
	 * Creating DB manager object and get logged in userID and according to that
	 * fetch liked feeds
	 */
	private void getLikedFeedsWithType() {
		this.feedLikeDAO = new FeedLikeDAO(UliveFeedListActivity.this);
		/** get logged in userID */
		this.baseApp = (BaseApplication) ((BaseActivity) UliveFeedListActivity.this)
				.getBaseApplication();
		loggedInUserID = this.baseApp.getLoggedInUserId();

		/** fetch feeds with feed type and userID */
		feedlikedlist = this.feedLikeDAO.read(loggedInUserID, mFeedType);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return super.onOptionsItemSelected(item);
	}

	private void postUliveFeed(String requestUrl, final int feedType) {

		Log.d("Post Request Send to Server", "" + requestUrl);
		NetworkCallback callback = new NetworkCallback() {

			@Override
			public void onSuccess(final String jsonResponse) {
				try {

					UliveFeedListActivity.this.parseUliveFeed(jsonResponse,
							feedType);
					Log.d("Result Received from Server", "" + jsonResponse);
					UliveFeedListActivity.this.mProgressDialog.dismiss();
				} catch (JsonParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onError(final int httpErrorCode) {
				/**
				 * If we get a HTTP code other than 200 , display an error
				 * message.
				 */
				if (null != UliveFeedListActivity.this.mProgressDialog) {
					UliveFeedListActivity.this.mProgressDialog.dismiss();
				}
				if (HttpStatus.OK.value() != httpErrorCode) {
					if (NetworkUtil.isConnected(UliveFeedListActivity.this
							.getApplicationContext())) {
						Toast.makeText(UliveFeedListActivity.this,
								R.string.try_again_later, Toast.LENGTH_LONG)
								.show();
					} else {
						Toast.makeText(UliveFeedListActivity.this,
								R.string.no_internet_connection,
								Toast.LENGTH_LONG).show();
					}
				}

			}
		};

		Hashtable<String, String> headers = new Hashtable<>();

		headers.put(BFS_TARGET_HOST, ULIVE_HOST);

		UliveRestCall call = new UliveRestCall(UliveFeedListActivity.this,
				callback, HttpMethod.POST, headers);

		/** Check whether Connected to Internet */
		if (NetworkUtil.isConnected(UliveFeedListActivity.this)) {

			try {
				call.execute(new URL(requestUrl));
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			/* Show toast message */
			Toast.makeText(
					UliveFeedListActivity.this,
					UliveFeedListActivity.this
							.getString(R.string.internet_not_available),
					Toast.LENGTH_LONG).show();
		}
	}

	private void parseUliveFeed(final String jsonResponse, final int feedType)
			throws JsonParseException, JsonMappingException, IOException {

		ArrayList<ParseDataNotFoundJsonResponse> dataNotFound = null;
		ArrayList<ParseJsonErrorResponse> error = null;

		ParseUliveFeed obj = (ParseUliveFeed) JacksonUtils.fromJsonToJava(
				jsonResponse, ParseUliveFeed.class);
		if (null != obj) {

			mList = obj.getList();
			
			if (mList==null || mList.size() == 0){
				mTxtNoFeeds.setVisibility(View.VISIBLE);
				return;
			}
			if (feedType == UliveFeed.FEED_TYPE_CRITICAL_ANNOUNCEMENT && mList.size() > 0) {
				FeedListCustomAdapter adapter = new FeedListCustomAdapter(this,
						mList, UliveFeed.FEED_TYPE_CRITICAL_ANNOUNCEMENT,
						this.feedlikedlist, this.feedLikeDAO, loggedInUserID, this.mDocumentList);
				mFeedList.setAdapter(adapter);
			} else if (feedType == UliveFeed.FEED_TYPE_PURE_LIFE && mList.size() > 0) {
				FeedListCustomAdapter adapter = new FeedListCustomAdapter(this,
						mList, UliveFeed.FEED_TYPE_PURE_LIFE,
						this.feedlikedlist, this.feedLikeDAO, loggedInUserID, this.mDocumentList);
				mFeedList.setAdapter(adapter);
			} else if (feedType == UliveFeed.FEED_TYPE_MD_DESK && mList.size() > 0) {
				FeedListCustomAdapter adapter = new FeedListCustomAdapter(this,
						mList, UliveFeed.FEED_TYPE_MD_DESK, this.feedlikedlist,
						this.feedLikeDAO, loggedInUserID, this.mDocumentList);
				mFeedList.setAdapter(adapter);
			} else if (feedType == UliveFeed.FEED_TYPE_ORG_ANNOUNCEMENT && mList.size() > 0) {
				FeedListCustomAdapter adapter = new FeedListCustomAdapter(this,
						mList,
						UliveFeed.FEED_TYPE_ORG_ANNOUNCEMENT,
						this.feedlikedlist, this.feedLikeDAO, loggedInUserID, this.mDocumentList);
				mFeedList.setAdapter(adapter);
			} else if (feedType == UliveFeed.FEED_TYPE_ARCHIVAL && mList.size() > 0) {
				FeedListCustomAdapter adapter = new FeedListCustomAdapter(this,
						mList,
						UliveFeed.FEED_TYPE_ARCHIVAL,
						this.feedlikedlist, this.feedLikeDAO, loggedInUserID, this.mDocumentList);
				mFeedList.setAdapter(adapter);
			}

			

		} else {
			mTxtNoFeeds.setVisibility(View.VISIBLE);
		}

		
	}

	@Override
	public void initViews() {
		FrameLayout frameLayout = (FrameLayout) findViewById(R.id.content_frame);

		LayoutInflater layoutInflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.activityView = layoutInflater.inflate(
				R.layout.activity_ulive_feed_list, null, false);
		// add the custom layout of this activity to frame layout.
		frameLayout.addView(this.activityView);

		this.mProgressDialog = ProgressDialog.show(UliveFeedListActivity.this,
				"", "Loading Data...", true);
		this.mProgressDialog.setCanceledOnTouchOutside(false);
		mFeedList = (ListView) activityView.findViewById(R.id.lst_feed_list);

		mTxtNoFeeds = (TextView) activityView.findViewById(R.id.txtnofeeds);
		mFeedList.setOnItemClickListener(this);
	}

	@Override
	public void updateViews() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub

		Intent intent = new Intent(UliveFeedListActivity.this,
				ULiveFeedDetailsActivity.class);
		intent.putExtra(Constants.FEED_TYPE, mFeedType);

		switch (mFeedType) {
		case UliveFeed.FEED_TYPE_CRITICAL_ANNOUNCEMENT:

			CriticalAnnouncement objCriticalAnnouncement = (CriticalAnnouncement) mList
					.get(position);
			intent.putExtra(Constants.CRITICAL_ANNOUNCEMENT,
					objCriticalAnnouncement);
			break;

		case UliveFeed.FEED_TYPE_PURE_LIFE:

			PureLife objPureLife = (PureLife) mList.get(position);
			intent.putExtra(Constants.PURE_LIFE, objPureLife);
			break;

		case UliveFeed.FEED_TYPE_ORG_ANNOUNCEMENT:

			FeedCentral objFeedCentral = (FeedCentral) mList.get(position);
			intent.putExtra(Constants.FEED_CENTRAL_OF_TODAY, objFeedCentral);
			break;

		case UliveFeed.FEED_TYPE_ARCHIVAL:

			FeedCentral objFeedCentralArchival = (FeedCentral) mList
					.get(position);
			intent.putExtra(Constants.FEED_CENTRAL_OF_SEVEN_DAYS,
					objFeedCentralArchival);
			break;

		case UliveFeed.FEED_TYPE_MD_DESK:

			MDsDesk objMDsDesk = (MDsDesk) mList.get(position);
			intent.putExtra(Constants.MD_DESK, objMDsDesk);
			break;

		default:
			return;

		}

		startActivity(intent);
	}

	private void post(String documentCategory) {

		String request = Constants.ULIVE_DEPARTMENT_DOCUMENT_URL
				+ "documentCategory=" + documentCategory + "&Source=One App";

		Log.d("Post Request Send to Server", "" + request);
		NetworkCallback callback = new NetworkCallback() {

			@Override
			public void onSuccess(final String jsonResponse) {
				try {

					UliveFeedListActivity.this.parse(jsonResponse);
					Log.d("Result Received from Server", "" + jsonResponse);
				} catch (JsonParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onError(final int httpErrorCode) {
				/**
				 * If we get a HTTP code other than 200 , display an error
				 * message.
				 */
				// UliveDocumentViewActivity.this.progress.dismiss();

				if (HttpStatus.OK.value() != httpErrorCode) {
					if (NetworkUtil.isConnected(UliveFeedListActivity.this
							.getApplicationContext())) {
//						Toast.makeText(UliveFeedListActivity.this,
//								R.string.try_again_later, Toast.LENGTH_LONG)
//								.show();
					} else {
//						Toast.makeText(UliveFeedListActivity.this,
//								R.string.no_internet_connection,
//								Toast.LENGTH_LONG).show();
					}
				}

			}
		};

		Hashtable<String, String> headers = new Hashtable<>();

		headers.put(BFS_TARGET_HOST, ULIVE_DOCUMENT_DEPARTMENT_HOST);

		UliveRestCall call = new UliveRestCall(UliveFeedListActivity.this,
				callback, HttpMethod.POST, headers);
		try {
			call.execute(new URL(request));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void parse(final String jsonResponse) throws JsonParseException,
			JsonMappingException, IOException {
		ArrayList<ParseDataNotFoundJsonResponse> dataNotFound = null;
		ArrayList<ParseJsonErrorResponse> error = null;

		ParseDocumentRepositoryResponseMethodName obj = (ParseDocumentRepositoryResponseMethodName) JacksonUtils
				.fromJsonToJava(jsonResponse,
						ParseDocumentRepositoryResponseMethodName.class);
		if (null != obj) {
			ParseDocumentRepositoryResponseTag tag = (ParseDocumentRepositoryResponseTag) JacksonUtils
					.fromJsonToJava(obj.getMethodName(),
							ParseDocumentRepositoryResponseTag.class);
			if (null != tag) {
				ParseDocumentRepositoryResponseTable response = tag.getTable();
				dataNotFound = response.getDataNotFound();
				error = response.getErrorResponse();
				if ((null != dataNotFound) && (dataNotFound.size() > 0)) {
					String dataNotFoundMessage = dataNotFound.get(0)
							.getStatus();
					if (null != dataNotFoundMessage) {
						
//						Toast.makeText(UliveFeedListActivity.this,
//								R.string.try_again_later, Toast.LENGTH_LONG)
//								.show();
					}
				} else if ((null != error) && (error.size() > 0)) {
					String errorMessage = error.get(0).getStatus();
					if (null != errorMessage) {
						
//						Toast.makeText(UliveFeedListActivity.this,
//								R.string.try_again_later, Toast.LENGTH_LONG)
//								.show();
					}
				} else if (null != response) {
					this.mDocumentList = response.getList();
					if ((null != this.mDocumentList) && (this.mDocumentList.size() > 0)) {
						
					}
				}
			}
		}
	}

}
