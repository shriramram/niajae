package net.atos.ulive.adapters;

import java.util.ArrayList;
import java.util.List;

import net.atos.ulive.R;
import net.atos.ulive.ULiveFeedLike;
import net.atos.ulive.dao.FeedLikeDAO;
import net.atos.ulive.dao.FeedLikeSchema;
import net.atos.ulive.dto.CriticalAnnouncement;
import net.atos.ulive.dto.DocumentRepositoryDTO;
import net.atos.ulive.dto.FeedCentral;
import net.atos.ulive.dto.FeedLikedDTO;
import net.atos.ulive.dto.MDsDesk;
import net.atos.ulive.dto.PureLife;
import net.atos.ulive.interfaces.UliveFeed;
import net.atos.ulive.views.UliveDocumentViewActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class FeedListCustomAdapter extends BaseAdapter {

	private Context mContext;
	private ArrayList<UliveFeed> mList;
	private int mFeedType;
	FeedLikeDAO feedLikeDAO = null;
	private int loggedInUserID;
	private List<FeedLikedDTO> feedlikedlist = null;
	private List<DocumentRepositoryDTO> docList = null;
	private ULiveFeedLike uLiveFeedLike = null;
	private String feedType = "";

	public FeedListCustomAdapter(Context context, List<UliveFeed> rowItems,
			int feedType, List<FeedLikedDTO> feedlikedlist,
			FeedLikeDAO feedLikeDAO, int userID,
			List<DocumentRepositoryDTO> documentList) {
		this.mContext = context;
		this.mList = (ArrayList<UliveFeed>) rowItems;
		this.mFeedType = feedType;
		this.feedLikeDAO = feedLikeDAO;
		this.loggedInUserID = userID;
		this.feedlikedlist = feedlikedlist;
		this.docList = documentList;
		this.uLiveFeedLike = new ULiveFeedLike(context);
	}

	@Override
	public int getCount() {
		int size = 0;
		if (null != mList) {
			size = mList.size();
		}
		return size;
	}

	@Override
	public Object getItem(int position) {
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mList.indexOf(getItem(position));
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View vi = convertView;
		final ViewHolder holder;
		LayoutInflater mInflater = (LayoutInflater) mContext
				.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

		if (convertView == null) {
			vi = mInflater.inflate(R.layout.ulive_list_item, null);

			holder = new ViewHolder();

			holder.txtModuleDisplayName = (TextView) vi
					.findViewById(R.id.txtModuleDisplayName);
			holder.txtFeedTitle = (TextView) vi.findViewById(R.id.txtFeedTitle);

			/** To Underline the Feed Title */
			holder.txtFeedTitle.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

			holder.txtFeedDescription = (TextView) vi
					.findViewById(R.id.txtFeedDescription);

			holder.chklike = (ImageButton) vi.findViewById(R.id.chklike);

			holder.txtFeedTitle.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String urlToBeHit = "";

					if (docList != null) {
						for (DocumentRepositoryDTO documentRepositoryDTO : docList) {
							if (holder.txtFeedTitle.getText().toString().trim()
									.equalsIgnoreCase(
											documentRepositoryDTO.getTitle())) {
								urlToBeHit = documentRepositoryDTO
										.getVirtualPath();
								Log.d("ULIVE >>> ",
										documentRepositoryDTO.getTitle());
							}
						}
					}

					if (!urlToBeHit.isEmpty()) {
						Intent intent = new Intent(mContext,
								UliveDocumentViewActivity.class);
						intent.putExtra("URL", urlToBeHit);
						intent.putExtra("Feed Title", holder.txtFeedTitle.getText().toString());
						mContext.startActivity(intent);
					} else {
						Toast.makeText(mContext, "No Document for this Feed",
								Toast.LENGTH_LONG).show();
					}
				}
			});

			holder.chklike.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					/** Store feed values in a dto for insertion and deletion */
					FeedLikedDTO dto = new FeedLikedDTO();
					dto.setFeedTitle(holder.txtFeedTitle.getText().toString()
							.trim());
					dto.setFeedType(mFeedType);
					dto.setLoggedInUserID(loggedInUserID);

					/** check whether click feed is liked or not */
					boolean isAvailable = markedAsLiked(holder.txtFeedTitle
							.getText().toString().trim(), holder.chklike);
					if (!isAvailable) {
						/** insert record into database and feed liked list */
						insertLikedFeed(dto);
						// Toast.makeText(mContext, "Feed Liked.",
						// Toast.LENGTH_SHORT).show();
						holder.chklike.setImageResource(R.drawable.ic_like);
						uLiveFeedLike.postFeedLike(true, dto, feedType);
					} else {
						/** delete from feed like list and database */
						deleteLikedFeed(dto);
						// Toast.makeText(mContext, "Feed Unliked.",
						// Toast.LENGTH_SHORT).show();
						holder.chklike.setImageResource(R.drawable.like_button);
						uLiveFeedLike.postFeedLike(false, dto, feedType);
					}
				}
			});
			vi.setTag(holder);

		} else {
			holder = (ViewHolder) vi.getTag();
		}

		switch (mFeedType) {

		case UliveFeed.FEED_TYPE_CRITICAL_ANNOUNCEMENT:
			// Set data for critical announcement
			CriticalAnnouncement objCriticalAnnouncement = (CriticalAnnouncement) mList
					.get(position);
			holder.txtModuleDisplayName.setText(objCriticalAnnouncement
					.getIconTitle());
			holder.txtFeedDescription.setText(objCriticalAnnouncement
					.getDescriptor());
			holder.txtFeedTitle.setText(objCriticalAnnouncement.getTitle());
			feedType = objCriticalAnnouncement.getType();
			Log.d("Type >>> ", objCriticalAnnouncement.getType());
			/**
			 * check whether this feed is liked or not if it is set liked
			 * otherwise set disliked
			 */
			markedAsLiked(objCriticalAnnouncement.getTitle().toString().trim(),
					holder.chklike);
			break;

		case UliveFeed.FEED_TYPE_PURE_LIFE:
			// Set data for pure life
			PureLife objPureLife = (PureLife) mList.get(position);
			holder.txtModuleDisplayName.setText(objPureLife.getModuleName());
			holder.txtFeedDescription.setText(objPureLife
					.getPureLifeDescription());
			holder.txtFeedTitle.setText(objPureLife.getTitle());
			feedType = objPureLife.getType();
			Log.d("Type >>> ", objPureLife.getType());
			/**
			 * check whether this feed is liked or not if it is set liked
			 * otherwise set disliked
			 */
			markedAsLiked(objPureLife.getTitle().toString().trim(),
					holder.chklike);
			break;

		case UliveFeed.FEED_TYPE_MD_DESK:
			// Set data for md's desk
			MDsDesk objMDsDesk = (MDsDesk) mList.get(position);
			holder.txtModuleDisplayName.setText(objMDsDesk.getIconText());
			holder.txtFeedDescription.setText(objMDsDesk.getMDDescription());
			holder.txtFeedTitle.setText(objMDsDesk.getMDTitle());
			feedType = objMDsDesk.getType();
			Log.d("Type >>> ", objMDsDesk.getType());
			/**
			 * check whether this feed is liked or not if it is set liked
			 * otherwise set disliked
			 */
			markedAsLiked(objMDsDesk.getMDTitle().toString().trim(),
					holder.chklike);
			break;

		case UliveFeed.FEED_TYPE_ORG_ANNOUNCEMENT:
		case UliveFeed.FEED_TYPE_ARCHIVAL:
			// Set data for pure life
			FeedCentral objFeedCentral = (FeedCentral) mList.get(position);
			holder.txtModuleDisplayName.setText(objFeedCentral
					.getModuleDisplayName());
			holder.txtFeedDescription.setText(objFeedCentral
					.getFeedDescription());
			holder.txtFeedTitle.setText(objFeedCentral.getFeedTitle());
			feedType = objFeedCentral.getType();
			Log.d("Type >>> ", objFeedCentral.getType());
			/**
			 * check whether this feed is liked or not if it is set liked
			 * otherwise set disliked
			 */
			markedAsLiked(objFeedCentral.getFeedTitle().toString().trim(),
					holder.chklike);
			break;

		default:

		}

		return vi;
	}

	/**
	 * @param dto
	 *            delete disliked record from database and also remove from the
	 *            feed like list
	 */
	protected void deleteLikedFeed(FeedLikedDTO dto) {
		/** remove record from feed like list */
		if(feedlikedlist !=null){
			for (int i = 0; i < feedlikedlist.size(); i++) {
				if (feedlikedlist.get(i).getFeedTitle()
						.equalsIgnoreCase(dto.getFeedTitle())) {
					feedlikedlist.remove(i);
				}
			}
		}
		feedLikeDAO.delete(FeedLikeSchema.TABLE, dto);
	}

	/**
	 * @param dto
	 *            insert like record into database and also insert into feed
	 *            like list
	 */
	protected void insertLikedFeed(FeedLikedDTO dto) {
		/** insert like record into database and also insert into feed like list */
		if(feedlikedlist !=null){
			feedlikedlist.add(dto);
		}
		feedLikeDAO.insert(FeedLikeSchema.TABLE, dto);
	}

	/**
	 * 
	 * TO compare list feed title with fetched list title from database
	 * 
	 * @param feed_title
	 * @param chklike
	 */
	private boolean markedAsLiked(String feed_title, ImageButton chklike) {

		boolean isSelect = false;
		if (feedlikedlist != null && feedlikedlist.size() != 0) {
			/**
			 * compare feed title and if it is matched set as liked and break it
			 * other-wise continue
			 */
			for (FeedLikedDTO feedLikedDTO : feedlikedlist) {
				if (feedLikedDTO.getFeedTitle().equalsIgnoreCase(feed_title)) {
					isSelect = true;
					break;
				}
			}
		}
		/**
		 * check whether this feed is liked or not if it is available in list
		 * set as liked otherwise set as a disliked
		 */
		if (isSelect) {
			chklike.setImageResource(R.drawable.ic_like);
		} else {
			chklike.setImageResource(R.drawable.like_button);
		}
		return isSelect;
	}

	public static class ViewHolder {

		/** Name of the Module to be displayed. */
		public TextView txtModuleDisplayName;

		/** Title of the Feed. */
		public TextView txtFeedTitle;

		/** Description of the Feed. */
		public TextView txtFeedDescription;

		/** Like ImageButton */

		public ImageButton chklike;

		public ViewHolder() {
			super();
			// TODO Auto-generated constructor stub
		}
	}

}
