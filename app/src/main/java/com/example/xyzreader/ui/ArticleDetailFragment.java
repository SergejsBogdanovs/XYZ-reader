package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_TRANSITION_POSITION = "transition_position";
    public static final String ARG_STARTING_TRANSITION_POSITION = "starting_transition_position";

    @BindView(R.id.photo)
    ImageView mPhotoView;

    @Nullable
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @Nullable
    @BindView(R.id.collapsing_toolbar)
    CollapsingToolbarLayout mCollapsingToolbar;

    @Nullable
    @BindView(R.id.app_bar)
    AppBarLayout mAppbar;

    @BindView(R.id.article_title)
    TextView mArticleTitle;

    @Nullable
    @BindView(R.id.article_byline)
    TextView mArticleByline;

    @BindView(R.id.meta_bar)
    LinearLayout mMetaBar;

    @BindView(R.id.article_body)
    TextView mArticleBody;

    @BindView(R.id.main_content)
    NestedScrollView mMainContent;

    @BindView(R.id.share_fab)
    FloatingActionButton mShareFab;

    @Nullable
    @BindView(R.id.card)
    CardView mCard;

    Unbinder unbinder;

    private View mRootView;
    private Cursor mCursor;
    private long mItemId;
    private int mCurrentPosition;
    private int mStartingPosition;
    private boolean mIsTransitioning;
    private int mMutedColor = 0xFF333333;
    private long mBackgroundImageFadeMillis;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId, int position, int startingPosition) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putInt(ARG_TRANSITION_POSITION, position);
        arguments.putInt(ARG_STARTING_TRANSITION_POSITION, startingPosition);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mStartingPosition = getArguments().getInt(ARG_STARTING_TRANSITION_POSITION);
        mCurrentPosition = getArguments().getInt(ARG_TRANSITION_POSITION);
        mIsTransitioning = savedInstanceState == null && mStartingPosition == mCurrentPosition;
        mBackgroundImageFadeMillis = getResources().getInteger(
                R.integer.fragment_details_background_image_fade_millis);

        setHasOptionsMenu(true);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        unbinder = ButterKnife.bind(this, mRootView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPhotoView.setTransitionName(getString(R.string.transition_image) + mCurrentPosition);
            mPhotoView.setTag(getString(R.string.transition_image) + mCurrentPosition);
        }

        mShareFab.setOnClickListener(v -> startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                .setType("text/plain")
                .setText("Some sample text")
                .getIntent(), getString(R.string.action_share))));

        bindViews();
        return mRootView;
    }

    public void startPostponedEnterTransition() {
        if (mCurrentPosition == mStartingPosition) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                        ActivityCompat.startPostponedEnterTransition(getActivity());
                        return true;
                    }
                });
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }


    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        mArticleByline.setMovementMethod(new LinkMovementMethod());

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);

            // Title
            final String title = mCursor.getString(ArticleLoader.Query.TITLE);
            mArticleTitle.setText(title);

            // Body
            final String body = Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)).toString();
            mArticleBody.setText(body);

            // Author and Date
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            Date parsedDate = parsePublishedDate(date);
            String author;
            if (!parsedDate.before(START_OF_EPOCH.getTime())) {
                author = Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                parsedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)).toString();
            } else {
                author = Html.fromHtml(
                        outputFormat.format(parsedDate)
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)).toString();
            }
            mArticleByline.setText(author);

            // Image
            String photoUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(photoUrl, new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.from(bitmap).generate();
                                mMutedColor = p.getDarkMutedColor(0xFF424242);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                            }
                            startPostponedEnterTransition();
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                        }
                    });

            if (mToolbar != null) {
                if (mCard == null) {
                    mToolbar.setTitle(title);
                }
                mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                mToolbar.setNavigationOnClickListener(v -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getActivity().finishAfterTransition();
                    }
                    getActivity().onBackPressed();
                });
            }
        } else {
            mRootView.setVisibility(View.GONE);
            mArticleTitle.setText("N/A");
            mArticleByline.setText("N/A");
            mArticleBody.setText("N/A");
        }
    }

    private Date parsePublishedDate(String date) {
        try {
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            return new Date();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public ImageView getAlbumImage() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mPhotoView)) {
            return mPhotoView;
        }
        return null;
    }

    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }
}
