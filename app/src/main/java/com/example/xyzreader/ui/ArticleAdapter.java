package com.example.xyzreader.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_STARTING_POSITION;

class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder> {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    private Context mContext;
    private Cursor mCursor;
    private int mArticlePosition;

    public ArticleAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_article, parent, false);
        final ViewHolder vh = new ViewHolder(view);

        view.setOnClickListener(v -> {
            mArticlePosition = vh.getAdapterPosition();

            ActivityOptionsCompat activityOptionsCompat = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        (Activity) mContext,
                        vh.thumbnailView,
                        vh.thumbnailView.getTransitionName());
            }

            Intent intent = new Intent(Intent.ACTION_VIEW,
                    ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
            intent.putExtra(EXTRA_STARTING_POSITION, mArticlePosition);
            mContext.startActivity(intent, activityOptionsCompat.toBundle());
        });

        return vh;
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            return new Date();
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        mArticlePosition = position;

        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

        Date publishedDate = parsePublishedDate();
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {

            holder.subtitleView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        } else {
            holder.subtitleView.setText(Html.fromHtml(
                    outputFormat.format(publishedDate)
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.thumbnailView.setTransitionName(mContext.getString(R.string.transition_image) + mArticlePosition);
            holder.thumbnailView.setTag(mContext.getString(R.string.transition_image) + mArticlePosition);
        }

        String imageUrl = mCursor.getString(ArticleLoader.Query.THUMB_URL);
        ImageLoader loader = ImageLoaderHelper.getInstance(mContext).getImageLoader();
        holder.thumbnailView.setImageUrl(imageUrl, loader);
        holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

        loader.get(imageUrl, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                Bitmap bitmap = imageContainer.getBitmap();
                if (bitmap != null) {
                    Palette p = Palette.from(bitmap).generate();
                    int mMutedColor = p.getDarkMutedColor(0xFF424242);
                    holder.cardView.setCardBackgroundColor(mMutedColor);
                }
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });

    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.thumbnail)
        DynamicHeightNetworkImageView thumbnailView;
        @BindView(R.id.article_title)
        TextView titleView;
        @BindView(R.id.article_subtitle)
        TextView subtitleView;
        @BindView(R.id.card_view)
        CardView cardView;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }
}

