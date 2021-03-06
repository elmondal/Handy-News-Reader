/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.yanus171.feedexfork.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.util.Date;
import java.util.regex.Matcher;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.adapter.DrawerAdapter;
import ru.yanus171.feedexfork.adapter.EntriesCursorAdapter;
import ru.yanus171.feedexfork.fragment.EntriesListFragment;
import ru.yanus171.feedexfork.fragment.GeneralPrefsFragment;
import ru.yanus171.feedexfork.parser.OPML;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.service.AutoJobService;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;

import static ru.yanus171.feedexfork.Constants.DB_AND;
import static ru.yanus171.feedexfork.Constants.DB_COUNT;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.FAVORITES_CONTENT_URI;
import static ru.yanus171.feedexfork.service.FetcherService.GetEnryUri;
import static ru.yanus171.feedexfork.service.FetcherService.GetExtrenalLinkFeedID;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_READ_ARTICLE_COUNT;

public class HomeActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String STATE_CURRENT_DRAWER_POS = "STATE_CURRENT_DRAWER_POS";
    private View mDimFrame;

    private static final String FEED_NUMBER( final String where ) {
        return "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " +
                where + DB_AND  + EntryColumns.FEED_ID + '=' + FeedColumns.TABLE_NAME + '.' + FeedColumns._ID + ')';
    }
    private static final String GROUP_NUMBER( final String where ) {
        if ( PrefUtils.getBoolean( "show_group_entries_count", false ) )
            return "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " +
                    where + DB_AND + EntryColumns.FEED_ID + " IN ( SELECT " + FeedColumns._ID +  " FROM " +
                    FeedColumns.TABLE_NAME + " AS t1"+ " WHERE " +
                    FeedColumns.GROUP_ID +  " = " + FeedColumns.TABLE_NAME + "." + FeedColumns._ID  + ") " + " )";
        else
        return "0";

    }
    private final String EXPR_NUMBER ( final String where ) {
        return "CASE WHEN " + FeedColumns.WHERE_GROUP + " THEN " + GROUP_NUMBER( where ) +
               " ELSE " + FEED_NUMBER( where ) + " END";
    }
    //private static final String FEED_ALL_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " +
    //        EntryColumns.FEED_ID + '=' + FeedColumns.TABLE_NAME + '.' + FeedColumns._ID + ')';


    private static final int LOADER_ID = 0;
    private static final int PERMISSIONS_REQUEST_IMPORT_FROM_OPML = 1;

    private EntriesListFragment mEntriesFragment;
    private DrawerLayout mDrawerLayout;
    private View mLeftDrawer;
    private ListView mDrawerList;
    private DrawerAdapter mDrawerAdapter = null;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;
    private int mCurrentDrawerPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timer timer = new Timer( "HomeActivity.onCreate" );
        super.onCreate(savedInstanceState);

        if ( getIntent().hasCategory( "LoadLinkLater" ) )
            finish();
        else
            setContentView(R.layout.activity_home);

        if ( PrefUtils.getLong( PrefUtils.FIRST_LAUNCH_TIME, -1 ) == -1 )
            PrefUtils.putLong( PrefUtils.FIRST_LAUNCH_TIME, System.currentTimeMillis() );

        mEntriesFragment = (EntriesListFragment) getSupportFragmentManager().findFragmentById(R.id.entries_list_fragment);

        mTitle = getTitle();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mLeftDrawer = findViewById(R.id.left_drawer);
        //mLeftDrawer.setBackgroundColor(ContextCompat.getColor( this, PrefUtils.IsLightTheme() ?  R.color.light_background : R.color.dark_background));

        mDrawerList = findViewById(R.id.drawer_list);
        mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectDrawerItem(position);
                CloseDrawer();
            }
        });
        mDrawerList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (id > 0) {
                    startActivity(new Intent(Intent.ACTION_EDIT).setData(FeedColumns.CONTENT_URI(id)));
                    return true;
                }
                return false;
            }
        });

        mCurrentDrawerPos = 0;
        if ( PrefUtils.getBoolean(PrefUtils.REMEBER_LAST_ENTRY, true) )
            mCurrentDrawerPos = PrefUtils.getInt(STATE_CURRENT_DRAWER_POS, mCurrentDrawerPos);


        mDrawerLayout = findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }

        //if (!PrefUtils.getBoolean(PrefUtils.REMEBER_LAST_ENTRY, true))
        //    selectDrawerItem(0);

        Timer.Start( LOADER_ID, "HomeActivity.initLoader" );
        getLoaderManager().initLoader(LOADER_ID, null, this);

        if (Build.VERSION.SDK_INT >= 21 )
            AutoJobService.init(this);


        if (PrefUtils.getBoolean(PrefUtils.REFRESH_ON_OPEN_ENABLED, false)) {
            if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                FetcherService.StartService(new Intent(HomeActivity.this, FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS));
            }
        }

        // Ask the permission to import the feeds if there is already one backup
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                new File(OPML.GetAutoBackupOPMLFileName()).exists()) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.storage_request_explanation).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ActivityCompat.requestPermissions(HomeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_IMPORT_FROM_OPML);
                    }
                });
                builder.show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_IMPORT_FROM_OPML);
            }
        }

        timer.End();
    }

    private void CloseDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.closeDrawer(mLeftDrawer);
                }
            }, 50);
        }
    }

    @Override
    public void onPause() {
        synchronized (EntriesCursorAdapter.mMarkAsReadList) {
            EntriesCursorAdapter.mMarkAsReadList.clear();//SetIsReadMakredList();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        Timer timer = new Timer("HomeActivity.onResume");
        final Intent intent = getIntent();
        setIntent( new Intent() );
        final String TEXT = MainApplication.getContext().getString(R.string.loadingLink) + "...";
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND) && intent.hasExtra(Intent.EXTRA_TEXT)) {
            final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            final Matcher m = HtmlUtils.HTTP_PATTERN.matcher(text);
            if (m.find()) {
                final String url = text.substring(m.start(), m.end());
                final String title = text.substring(0, m.start());
                LoadAndOpenLink(url, title, TEXT);
            }
        } else if (intent.getScheme() != null && intent.getScheme().startsWith("http")) {
            final String url = intent.getDataString();
            final String title = intent.getDataString();
            LoadAndOpenLink(url, title, TEXT);
        }

        if ( intent.getData() != null && intent.getData().equals( FAVORITES_CONTENT_URI ) ) {
            selectDrawerItem( 2 );
        } else if (PrefUtils.getBoolean(PrefUtils.REMEBER_LAST_ENTRY, true)) {
            String lastUri = PrefUtils.getString(PrefUtils.LAST_ENTRY_URI, "");
            if (!lastUri.isEmpty() && !lastUri.contains("-1")) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(lastUri)));
            }
        }

        if ( GeneralPrefsFragment.mSetupChanged ) {
            Timer.Start( LOADER_ID, "HomeActivity.restartLoader LOADER_ID" );
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }
        //if ( mDrawerAdapter != null  )
        //    selectDrawerItem( mCurrentDrawerPos );
        timer.End();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //outState.putInt(STATE_CURRENT_DRAWER_POS, mCurrentDrawerPos);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // We reset the current drawer position
        // selectDrawerItem(0);
        setIntent( intent );
    }

    private void LoadAndOpenLink(final String url, final String title, final String text) {
        final ContentResolver cr = MainApplication.getContext().getContentResolver();
        Uri entryUri = GetEnryUri( url );
        if ( entryUri == null ) {
            final String feedID = GetExtrenalLinkFeedID();
            Timer timer = new Timer( "LoadAndOpenLink insert" );
            ContentValues values = new ContentValues();
            values.put(EntryColumns.TITLE, title);
            values.put(EntryColumns.SCROLL_POS, 0);
            values.put(EntryColumns.DATE, (new Date()).getTime());
            values.put(EntryColumns.LINK, url);
            values.put(EntryColumns.ABSTRACT, text );
            values.put(EntryColumns.MOBILIZED_HTML, text );
            entryUri = cr.insert(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedID), values);
            entryUri = Uri.withAppendedPath( EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI( feedID ), entryUri.getLastPathSegment() );
            timer.End();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FetcherService.LoadLink(feedID, url, title, FetcherService.ForceReload.Yes,true, true);
                }
            }).start();
        }

        PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, entryUri.toString());//FetcherService.OpenLink(entryUri);

    }


    public void onBackPressed() {
        // Before exiting from app the navigation drawer is opened
        if (mDrawerLayout != null && !mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    public void onClickEditFeeds(View view) {
        startActivity(new Intent(this, EditFeedsListActivity.class));
    }

    public void onClickAdd(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_add_feed)
                .setItems(new CharSequence[]{getString(R.string.add_custom_feed), getString(R.string.google_news_title)}, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            startActivity(new Intent(Intent.ACTION_INSERT).setData(FeedColumns.CONTENT_URI));
                        } else {
                            startActivity(new Intent(HomeActivity.this, AddGoogleNewsActivity.class));
                        }
                    }
                });
        builder.show();
    }

    public void onClickSettings(View view) {
        startActivity(new Intent(this, GeneralPrefsActivity.class));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Timer.Start( LOADER_ID, "HomeActivity.onCreateLoader" );
        final String EXPR_FEED_ALL_NUMBER = PrefUtils.getBoolean( SHOW_READ_ARTICLE_COUNT, false ) ? EXPR_NUMBER( "1=1" ) : "0";
        CursorLoader cursorLoader =
                new CursorLoader(this,
                        FeedColumns.GROUPED_FEEDS_CONTENT_URI,
                        new String[]{FeedColumns._ID, FeedColumns.URL, FeedColumns.NAME,
                                FeedColumns.IS_GROUP, FeedColumns.ICON, FeedColumns.LAST_UPDATE,
                                FeedColumns.ERROR, EXPR_NUMBER( EntryColumns.WHERE_UNREAD ), EXPR_FEED_ALL_NUMBER, FeedColumns.SHOW_TEXT_IN_ENTRY_LIST,
                                FeedColumns.IS_GROUP_EXPANDED, FeedColumns.IS_AUTO_REFRESH, FeedColumns.OPTIONS},
                        "(" + FeedColumns.WHERE_GROUP + Constants.DB_OR + FeedColumns.GROUP_ID + Constants.DB_IS_NULL + Constants.DB_OR +
                                FeedColumns.GROUP_ID + " IN (SELECT " + FeedColumns._ID +
                                " FROM " + FeedColumns.TABLE_NAME +
                                " WHERE " + FeedColumns.IS_GROUP_EXPANDED + Constants.DB_IS_TRUE + "))" + FeedData.getWhereNotExternal(),
                        null,
                        null);
        cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
        return cursorLoader;
    }


    public static Uri mNewFeedUri = Uri.EMPTY;
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Timer.End( LOADER_ID );
        Timer timer = new Timer( "HomeActivity.onLoadFinished" );
        synchronized (GeneralPrefsFragment.mSetupChanged) {
            boolean needSelect = false;
            if (mDrawerAdapter != null ) {
                mDrawerAdapter.setCursor(cursor);
            } else {
                mDrawerAdapter = new DrawerAdapter(this, cursor);
                mDrawerList.setAdapter(mDrawerAdapter);
                // We don't have any menu yet, we need to display it
                needSelect = true;
            }
            if ( GeneralPrefsFragment.mSetupChanged ) {
                GeneralPrefsFragment.mSetupChanged = false;
                needSelect = true;
            }

            if ( !mNewFeedUri.equals( Uri.EMPTY ) ) {
                mCurrentDrawerPos = mDrawerAdapter.getItemPosition( Long.parseLong( mNewFeedUri.getLastPathSegment() ) );
                needSelect = true;
                mDrawerList.smoothScrollToPosition( mCurrentDrawerPos );
                CloseDrawer();
                mNewFeedUri = Uri.EMPTY;
            }

            if ( needSelect )
                mDrawerList.post(new Runnable() {
                    @Override
                    public void run() {
                        selectDrawerItem(mCurrentDrawerPos);
                    }
                });
        }
        timer.End();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mDrawerAdapter.setCursor(null);
    }

    private void selectDrawerItem(int position) {
        Timer timer = new Timer( "HomeActivity.selectDrawerItem" );

        mCurrentDrawerPos = position;

        Uri newUri;
        boolean showFeedInfo = true;

        switch (position) {
            case 0:
                newUri = EntryColumns.UNREAD_ENTRIES_CONTENT_URI;
                break;
            case 1:
                newUri = EntryColumns.CONTENT_URI;
                break;
            case 2:
                newUri = FAVORITES_CONTENT_URI;
                break;
            case 3:
                newUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI( GetExtrenalLinkFeedID() );
                showFeedInfo = false;
                break;
            default:
                long feedOrGroupId = mDrawerAdapter.getItemId(position);
                if (feedOrGroupId != -1) {
                    if (mDrawerAdapter.isItemAGroup(position)) {
                        //newUri = EntryColumns.ENTRIES_FOR_GROUP_CONTENT_URI(feedOrGroupId);
                        newUri = mEntriesFragment.mShowUnRead ? EntryColumns.UNREAD_ENTRIES_FOR_GROUP_CONTENT_URI(feedOrGroupId) : EntryColumns.ENTRIES_FOR_GROUP_CONTENT_URI(feedOrGroupId);
                    } else {
                        //newUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedOrGroupId);
                        newUri = mEntriesFragment.mShowUnRead ? EntryColumns.UNREAD_ENTRIES_FOR_FEED_CONTENT_URI(feedOrGroupId) : EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedOrGroupId);
                        showFeedInfo = false;
                    }
                } else
                    newUri = EntryColumns.UNREAD_ENTRIES_CONTENT_URI;
                mTitle = mDrawerAdapter.getItemName(position);

                break;

        }

        //if (!newUri.equals(mEntriesFragment.getUri()))
        mEntriesFragment.setData(newUri,
                showFeedInfo,
                false,
                mDrawerAdapter != null && mDrawerAdapter.isShowTextInEntryList(position));

        //mDrawerList.setSelection( position );
        mDrawerList.setItemChecked(position, true);
        //mDrawerList.smoothScrollToPositionFromTop(mFirstVisibleItem, 0, 0);

        // First open => we open the drawer for you
        if (PrefUtils.getBoolean(PrefUtils.FIRST_OPEN, true)) {
            PrefUtils.putBoolean(PrefUtils.FIRST_OPEN, false);
            if (mDrawerLayout != null) {
                mDrawerLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDrawerLayout.openDrawer(mLeftDrawer);
                    }
                }, 500);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.welcome_title)
                    .setItems(new CharSequence[]{getString(R.string.google_news_title), getString(R.string.add_custom_feed)}, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 1) {
                                startActivity(new Intent(Intent.ACTION_INSERT).setData(FeedColumns.CONTENT_URI));
                            } else {
                                startActivity(new Intent(HomeActivity.this, AddGoogleNewsActivity.class));
                            }
                        }
                    });
            builder.show();
        }

        // Set title & icon
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setHomeAsUpIndicator( 0 );
            switch (mCurrentDrawerPos) {
                case 0:
                    getSupportActionBar().setTitle(R.string.unread_entries);
                    SetActionbarIndicator( R.mipmap.ic_launcher);
                    break;
                case 1:
                    getSupportActionBar().setTitle(R.string.all_entries);
                    SetActionbarIndicator( R.drawable.cup_empty );
                    break;
                case 2:
                    getSupportActionBar().setTitle(R.string.favorites);
                    SetActionbarIndicator( R.drawable.cup_with_star);
                    break;
                case 3:
                    getSupportActionBar().setTitle(R.string.externalLinks);
                    SetActionbarIndicator( R.drawable.load_later );
                    break;
                default:
                    getSupportActionBar().setTitle(mTitle);
                    Drawable image = mDrawerAdapter.getItemIcon( position ) == null ?
                                     null :
                                     new BitmapDrawable( MainApplication.getContext().getResources(),
                                                         UiUtils.getScaledBitmap( mDrawerAdapter.getItemIcon( position ), 32 ) );
                    getSupportActionBar().setHomeAsUpIndicator( image );
                    break;
            }
        }

        PrefUtils.putInt(STATE_CURRENT_DRAWER_POS, mCurrentDrawerPos);

        // Put the good menu
        invalidateOptionsMenu();
        timer.End();
    }

    private void SetActionbarIndicator( int imageResource) {
        Bitmap original = BitmapFactory.decodeResource(getResources(), imageResource);
        int size = UiUtils.dpToPixel( 32 );
        Bitmap b = Bitmap.createScaledBitmap( original, size, size, false);
        Drawable d = new BitmapDrawable(getResources(), b);
        getSupportActionBar().setHomeAsUpIndicator( d );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_IMPORT_FROM_OPML: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    FetcherService.StartService( FetcherService.GetIntent( Constants.FROM_IMPORT ).putExtra( Constants.EXTRA_FILENAME, OPML.GetAutoBackupOPMLFileName() ) );
                }
                return;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
