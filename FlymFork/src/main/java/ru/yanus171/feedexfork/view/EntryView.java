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
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ru.yanus171.feedexfork.view;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;

import static ru.yanus171.feedexfork.utils.Theme.BUTTON_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.LINK_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.LINK_COLOR_BACKGROUND;
import static ru.yanus171.feedexfork.utils.Theme.QUOTE_BACKGROUND_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.QUOTE_LEFT_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.SUBTITLE_BORDER_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.SUBTITLE_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.TEXT_COLOR_BACKGROUND;

public class EntryView extends WebView implements Observer {

    private static final String TEXT_HTML = "text/html";
    private static final String HTML_IMG_REGEX = "(?i)<[/]?[ ]?img(.|\n)*?>";
    //private static final String BACKGROUND_COLOR = PrefUtils.IsLightTheme() ? "#f6f6f6" : "#181b1f";
    //private static final String QUOTE_BACKGROUND_COLOR = PrefUtils.IsLightTheme() ? "#e6e6e6" : "#383b3f";
    //private static final String QUOTE_LEFT_COLOR = PrefUtils.IsLightTheme() ? "#a6a6a6" : "#686b6f";


    private long mEntryId = -1;
    public Runnable mScrollChangeListener = null;

    //private static final String TEXT_COLOR_BRIGHTNESS = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, false) ? "#000000" : "#C0C0C0";

    private static String GetCSS() { return "<head><style type='text/css'> "
            + "body {max-width: 100%; margin: " + getMargins() + "; text-align:" + getAlign() + "; font-weight: " + getFontBold()
            + " color: " + Theme.GetTextColor() + "; background-color:" + Theme.GetColor( TEXT_COLOR_BACKGROUND, R.string.default_text_color_background ) + "; line-height: 120%} "
            + "* {max-width: 100%; word-break: break-word}"
            + "h1, h2 {font-weight: normal; line-height: 130%} "
            + "h1 {font-size: 170%; text-align:center; margin-bottom: 0.1em} "
            + "h2 {font-size: 140%} "
            + "a {color: " + Theme.GetColor( LINK_COLOR, R.string.default_link_color )  + "; background: " + Theme.GetColor( LINK_COLOR_BACKGROUND , R.string.default_text_color_background ) +
            ( PrefUtils.getBoolean( "underline_links", true ) ? "" : "; text-decoration: none" ) + "}"
            + "h1 {color: inherit; text-decoration: none}"
            + "img {display: inline;max-width: 100%;height: auto} "
            + "iframe {allowfullscreen;position:relative;top:0;left:0;width:100%;height:100%;}"
            + "pre {white-space: pre-wrap;} "
            + "blockquote {border-left: thick solid " + Theme.GetColor( QUOTE_LEFT_COLOR, android.R.color.black ) + "; background-color:" + Theme.GetColor( QUOTE_BACKGROUND_COLOR, android.R.color.black  ) + "; margin: 0.5em 0 0.5em 0em; padding: 0.5em} "
            + "p {margin: 0.8em 0 0.8em 0} "
            + "p.subtitle {color: " + Theme.GetColor( SUBTITLE_COLOR, android.R.color.black  ) + "; border-top:1px " + Theme.GetColor( SUBTITLE_BORDER_COLOR, android.R.color.black  ) + "; border-bottom:1px " + Theme.GetColor( SUBTITLE_BORDER_COLOR, android.R.color.black ) + "; padding-top:2px; padding-bottom:2px; font-weight:800 } "
            + "ul, ol {margin: 0 0 0.8em 0.6em; padding: 0 0 0 1em} "
            + "ul li, ol li {margin: 0 0 0.8em 0; padding: 0} "
            + "div.button-section {padding: 0.4cm 0; margin: 0; text-align: center} "
            + ".button-section p {margin: 0.1cm 0 0.2cm 0}"
            + ".button-section p.marginfix {margin: 0.2cm 0 0.2cm 0}"
            + ".button-section input, .button-section a {font-family: sans-serif-light; font-size: 100%; color: #FFFFFF; background-color: " + Theme.GetColor( BUTTON_COLOR, android.R.color.black  ) + "; text-decoration: none; border: none; border-radius:0.2cm; padding: 0.3cm} "
            + "</style><meta name='viewport' content='width=device-width'/></head>"; }

    private static String getFontBold() {
        if ( PrefUtils.getBoolean( PrefUtils.ENTRY_FONT_BOLD, false ) )
            return "bold;";
        else
            return "normal;";
    }

    private static String getMargins() {
        if ( PrefUtils.getBoolean( PrefUtils.ENTRY_MAGRINS, true ) )
            return "4%";
        else
            return "0.1cm";
    }

    private static String getAlign() {
        if ( PrefUtils.getBoolean( PrefUtils.ENTRY_TEXT_ALIGN_JUSTIFY, false ) )
            return "justify";
        else
            return "left";
    }

    private static final String BODY_START = "<body>";
    private static final String BODY_END = "</body>";
    private static final String TITLE_START = "<h1>";
    //private static final String TITLE_MIDDLE = "'>";
    private static final String TITLE_END = "</h1>";
    private static final String SUBTITLE_START = "<p class='subtitle'>";
    private static final String SUBTITLE_END = "</p>";
    private static final String BUTTON_SECTION_START = "<div class='button-section'>";
    private static final String BUTTON_SECTION_END = "</div>";
    private static final String BUTTON_START = "<p><input type='button' value='";
    private static final String BUTTON_MIDDLE = "' onclick='";
    private static final String BUTTON_END = "'/></p>";
    // the separate 'marginfix' selector in the following is only needed because the CSS box model treats <input> and <a> elements differently
    private static final String LINK_BUTTON_START = "<p class='marginfix'><a href='";
    private static final String LINK_BUTTON_MIDDLE = "'>";
    private static final String LINK_BUTTON_END = "</a></p>";
    private static final String IMAGE_ENCLOSURE = "[@]image/";

    private final JavaScriptObject mInjectedJSObject = new JavaScriptObject();
    private final ImageDownloadJavaScriptObject mImageDownloadObject = new ImageDownloadJavaScriptObject();
    public static final ImageDownloadObservable mImageDownloadObservable = new ImageDownloadObservable();
    private EntryViewManager mEntryViewMgr;
    public String mData = "";
    public float mScrollPartY = 0;

    private EntryActivity mActivity;


    public EntryView(Context context) {
        super(context);
        init();
    }

    public EntryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EntryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setListener(EntryViewManager manager) {
        mEntryViewMgr = manager;
    }

    public void setHtml(long entryId,
                        String title,
                        String link,
                        String contentText,
                        String enclosure,
                        String author,
                        long timestamp,
                        final boolean preferFullText,
                        EntryActivity activity) {
        Timer timer = new Timer( "EntryView.setHtml" );

        mActivity = activity;
        mEntryId = entryId;
        //getSettings().setBlockNetworkLoads(true);
        getSettings().setUseWideViewPort( true );
        getSettings().setSupportZoom( false );
        getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        if (PrefUtils.getBoolean(PrefUtils.DISPLAY_IMAGES, true)) {
            contentText = HtmlUtils.replaceImageURLs(contentText, entryId, true);
            if (getSettings().getBlockNetworkImage()) {
                // setBlockNetworkImage(false) calls postSync, which takes time, so we clean up the html first and change the value afterwards
                loadData("", TEXT_HTML, Constants.UTF8);
                getSettings().setBlockNetworkImage(false);
            }
        } else {
            contentText = contentText.replaceAll(HTML_IMG_REGEX, "");
            getSettings().setBlockNetworkImage(true);
        }


        // String baseUrl = "";
        // try {
        // URL url = new URL(mLink);
        // baseUrl = url.getProtocol() + "://" + url.getHost();
        // } catch (MalformedURLException ignored) {
        // }
        // do not put 'null' to the base url...
        mData = generateHtmlContent(title, link, contentText, enclosure, author, timestamp, preferFullText);
        loadDataWithBaseURL("", mData, TEXT_HTML, Constants.UTF8, null);
        timer.End();
    }

    private String generateHtmlContent(String title, String link, String contentText, String enclosure, String author, long timestamp, boolean preferFullText) {
        Timer timer = new Timer( "EntryView.generateHtmlContent" );

        StringBuilder content = new StringBuilder(GetCSS()).append(BODY_START);

        if (link == null) {
            link = "";
        }
        content.append(TITLE_START).append(title).append(TITLE_END).append(SUBTITLE_START);
        Date date = new Date(timestamp);
        Context context = getContext();
        StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getLongDateFormat(context).format(date)).append(' ').append(
                DateFormat.getTimeFormat(context).format(date));

        if (author != null && !author.isEmpty()) {
            dateStringBuilder.append(" &mdash; ").append(author);
        }

        content.append(dateStringBuilder).append(SUBTITLE_END).append(contentText).append(BUTTON_SECTION_START).append(BUTTON_START);

        if (!preferFullText) {
            content.append(context.getString(R.string.get_full_text)).append(BUTTON_MIDDLE).append("injectedJSObject.onClickFullText();");
        } else {
            content.append(context.getString(R.string.original_text)).append(BUTTON_MIDDLE).append("injectedJSObject.onClickOriginalText();");
        }
        content.append(BUTTON_END);

        if (enclosure != null && enclosure.length() > 6 && !enclosure.contains(IMAGE_ENCLOSURE)) {
            content.append(BUTTON_START).append(context.getString(R.string.see_enclosure)).append(BUTTON_MIDDLE)
                    .append("injectedJSObject.onClickEnclosure();").append(BUTTON_END);
        }

        /*if (link.length() > 0) {
            content.append(LINK_BUTTON_START).append(link).append(LINK_BUTTON_MIDDLE).append(context.getString(R.string.see_link)).append(LINK_BUTTON_END);
        }*/

        content.append(BUTTON_SECTION_END).append(BODY_END);

        timer.End();
        return content.toString();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void init() {

        Timer timer = new Timer( "EntryView.init" );

        mImageDownloadObservable.addObserver(this);
        // For scrolling
        setHorizontalScrollBarEnabled(false);
        getSettings().setUseWideViewPort(true);
        // For color
        setBackgroundColor(Color.parseColor(Theme.GetColor( TEXT_COLOR_BACKGROUND, android.R.color.black  )));
        // Text zoom level from preferences
        int fontSize = PrefUtils.getFontSize();
        if (fontSize != 0) {
            getSettings().setTextZoom(100 + (fontSize * 20));
        }

        // For javascript
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(mInjectedJSObject, mInjectedJSObject.toString());
        addJavascriptInterface(mImageDownloadObject, mImageDownloadObject.toString());

        // For HTML5 video
        setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // if a view already exists then immediately terminate the new one
                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                FrameLayout videoLayout = mEntryViewMgr.getVideoLayout();
                if (videoLayout != null) {
                    mCustomView = view;

                    setVisibility(View.GONE);
                    videoLayout.setVisibility(View.VISIBLE);
                    videoLayout.addView(view);
                    mCustomViewCallback = callback;

                    mEntryViewMgr.onStartVideoFullScreen();
                }
            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();

                if (mCustomView == null) {
                    return;
                }

                FrameLayout videoLayout = mEntryViewMgr.getVideoLayout();
                if (videoLayout != null) {
                    setVisibility(View.VISIBLE);
                    videoLayout.setVisibility(View.GONE);

                    // HideByScroll the custom view.
                    mCustomView.setVisibility(View.GONE);

                    // Remove the custom view from its container.
                    videoLayout.removeView(mCustomView);
                    mCustomViewCallback.onCustomViewHidden();

                    mCustomView = null;

                    mEntryViewMgr.onEndVideoFullScreen();
                }
            }
            @Override
            public Bitmap getDefaultVideoPoster() {
                Bitmap result = super.getDefaultVideoPoster();
                if ( result == null )
                    result =  BitmapFactory.decodeResource( MainApplication.getContext().getResources(), android.R.drawable.presence_video_online );
                return result;
            }

        });

        setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Context context = getContext();
                try {
                    if (url.startsWith(Constants.FILE_SCHEME)) {
                        File file = new File(url.replace(Constants.FILE_SCHEME, ""));
                        File extTmpFile = new File(context.getExternalCacheDir(), file.getName());
                        FileUtils.copy(file, extTmpFile);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(extTmpFile), "image/jpeg");
                        context.startActivity(intent);
                    } else if ( url.contains( "#" ) ) {
                        return false;
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        context.startActivity(intent);
                    }
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, R.string.cant_open_link, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }


            @Override
            public void onPageFinished(WebView view, String url) {
                Dog.v("main", "EntryView.this.scrollTo " + mScrollPartY);
                if (mScrollPartY != 0 /*&& getContentHeight() != getScrollY()*/ ) {
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            EntryView.this.scrollTo(0, GetScrollY() );
                        }
                        // Delay the scrollTo to make it work
                    }, 150);
                }
            }
        });

        timer.End();
    }


    /*@Override
    public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        if ( scrollY > 50 && clampedY ) {
            mActivity.setFullScreen(false, EntryActivity.GetIsActionBarHidden());
        }

    }*/

    @Override
    protected void onScrollChanged (int l, int t, int oldl, int oldt) {
        FetcherService.Status().HideByScroll();
        //int contentHeight = (int) Math.floor(GetContentHeight());
        //int webViewHeight = getMeasuredHeight();
        mActivity.mEntryFragment.UpdateFooter();
        if ( mScrollChangeListener != null )
            mScrollChangeListener.run();
    }

    public boolean IsScrolAtBottom() {
        return getScrollY() + getMeasuredHeight() >= (int) Math.floor(GetContentHeight());
    }
    @Override
    public void update(Observable observable, Object data) {
        if ( ( data != null ) && ( (Long)data == mEntryId ) )  {
            if ( GetViewScrollPartY() < mScrollPartY )
                mScrollPartY = GetViewScrollPartY();
            mData = HtmlUtils.replaceImageURLs(mData, mEntryId, false);
            loadDataWithBaseURL("", mData, TEXT_HTML, Constants.UTF8, null);
        //setScrollY( y );
        }
    }

    public static void NotifyToUpdate( final long entryId) {
        synchronized ( mImageDownloadObservable ) {
                UiUtils.RunOnGuiThread( new Runnable() {
                    @Override
                    public void run() {
                    synchronized ( mImageDownloadObservable ) {
                        mImageDownloadObservable.notifyObservers(entryId);
                    }
                    }
                }, 1000);
        }
    }

    public interface EntryViewManager {
        void onClickOriginalText();

        void onClickFullText();

        void onClickEnclosure();

        void onStartVideoFullScreen();

        void onEndVideoFullScreen();

        FrameLayout getVideoLayout();

        void downloadImage(String url);

        void downloadNextImages();
    }

    private class JavaScriptObject {
        @Override
        @JavascriptInterface
        public String toString() {
            return "injectedJSObject";
        }

        @JavascriptInterface
        public void onClickOriginalText() {
            mEntryViewMgr.onClickOriginalText();
        }

        @JavascriptInterface
        public void onClickFullText() {
            mEntryViewMgr.onClickFullText();
        }

        @JavascriptInterface
        public void onClickEnclosure() {
            mEntryViewMgr.onClickEnclosure();
        }
    }

    private class ImageDownloadJavaScriptObject {
        @Override
        @JavascriptInterface
        public String toString() {
            return "ImageDownloadJavaScriptObject";
        }
        @JavascriptInterface
        public void downloadImage( String url ) {
            mEntryViewMgr.downloadImage(url);
        }
        @JavascriptInterface
        public void downloadNextImages() {
            mEntryViewMgr.downloadNextImages();
        }
    }

    public static class ImageDownloadObservable extends Observable {
        @Override
        public boolean hasChanged () {
            return true;
        }
    }


    private int GetScrollY() {
        return GetContentHeight() * mScrollPartY != 0 ? ( int )( GetContentHeight() * mScrollPartY ) : 0;
    }

    public float GetContentHeight() {
        return getContentHeight() * getScale();
    }

    public float GetViewScrollPartY() {
        return getContentHeight() != 0 ? getScrollY() / GetContentHeight() : 0 ;
    }

    public void PageChange(int delta ) {
        ObjectAnimator anim = ObjectAnimator.ofInt(this, "scrollY", getScrollY(),
                (int) (getScrollY() + delta * getHeight() *
                        ( PrefUtils.getBoolean("page_up_down_90_pct", false) ? 0.9 : 0.98 ) ));
        anim.setDuration(450);
        anim.setInterpolator( new AccelerateDecelerateInterpolator() );
        anim.start();
    }



}