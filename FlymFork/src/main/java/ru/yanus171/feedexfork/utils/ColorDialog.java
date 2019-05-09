package ru.yanus171.feedexfork.utils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.view.ColorPreference;

//***********************************************************************************
public class ColorDialog implements SeekBar.OnSeekBarChangeListener {
	private static final String cKey = "ColorSlot";

	private static final int cMaxColor = 255;
	private static final String cDefaultColor = "#000000";//4294967296L;
	static final int cColorViewHeight = 50;

	private static final int cPad = 3;
	private static final int cRowCount = 2;
	private static final int cColumnCount = 8;
	private static final int cCellWidth = 30;

	private TextView mlabelRed = null;
	private TextView mlabelGreen = null;
	private TextView mlabelBlue = null;
	private TextView mlabelTransparency = null;
	private SeekBar msbRed = null;
	private SeekBar msbGreen = null;
	private SeekBar msbBlue = null;
	private SeekBar msbTransparency = null;
	private TextView mviewDialogColor = null;
	public ColorTB mColor = new ColorTB();

	private boolean Result = false;
	private String Title;
	private boolean IsTransparency = false;
	private int ColorMode;
	private boolean IsBackground;
	private boolean IsText;
	private boolean IsOnProgressChangedEnabled = true;

	private static final int cTextColorMode = 0;
	private static final int cBackgroundColorMode = 1;
	public static final String cTextLetter = "Text";

	private Context Context = null;

	// -------------------------------------------------------------------------
	public ColorDialog(Context context, ColorTB color, boolean isTransparency, boolean isText, boolean isBackground,
			String title) {
		Context = context;
		mColor = (ColorTB) color.clone();
		IsTransparency = isTransparency;
		IsText = isText;
		IsBackground = isBackground;
		Title = title;
	}

	// -------------------------------------------------------------------------
	public AlertDialog.Builder CreateBuilder() {
		ScrollView scrollView = new ScrollView(Context);
		LinearLayout layout = new LinearLayout(Context);
		scrollView.addView(layout);
		layout.setOrientation(LinearLayout.VERTICAL);

		LinearLayout hLayout = new LinearLayout(Context);
		hLayout.setOrientation(LinearLayout.HORIZONTAL);
		hLayout.setPadding( UiUtils.dpToPixel( 5 ), 0, 0, UiUtils.dpToPixel( 5 ));
		layout.addView(hLayout);

		mviewDialogColor = CreateDialogColor(hLayout, IsText, IsBackground);

		AddTextBackgroundSwitch(hLayout);

		AddColorTable(layout);

		mlabelRed = new TextView(Context);
		mlabelGreen = new TextView(Context);
		mlabelBlue = new TextView(Context);
		mlabelTransparency = new TextView(Context);
		msbRed = new SeekBar(Context);
		msbGreen = new SeekBar(Context);
		msbBlue = new SeekBar(Context);
		msbTransparency = new SeekBar(Context);
		if (!IsTransparency) {
			msbTransparency.setVisibility(View.GONE);
			mlabelTransparency.setVisibility(View.GONE);
		}
		AddColorSeekBar(R.string.red, layout, mlabelRed, msbRed);
		AddColorSeekBar(R.string.green, layout, mlabelGreen, msbGreen);
		AddColorSeekBar(R.string.blue, layout, mlabelBlue, msbBlue);
		AddColorSeekBar(R.string.transparency, layout, mlabelTransparency, msbTransparency);
		UpdateBars();

		AddHint(layout);

		AlertDialog.Builder builder = Theme.CreateDialog(Context);
		builder.setView(scrollView);
		builder.setTitle(Title);

		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Result = false;
				dialog.dismiss();
			}
		});
		// UpdateView();
		return builder;
	}

	// -------------------------------------------------------------------------
	public static TextView CreateDialogColor(ViewGroup layout, boolean isText, boolean isBackground) {
		TextView result = new TextView(layout.getContext());
		result.setTypeface(Typeface.DEFAULT_BOLD);
		result.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
		if (isText && isBackground) {
			result.setText(cTextLetter);
			result.setGravity(Gravity.CENTER);
			int px = UiUtils.dpToPixel( 5 );
			result.setPadding(px, px, px, px);
		} else {
			result.setText(" ");
		}
		layout.addView(result, cColorViewHeight * 2, LayoutParams.FILL_PARENT);
		return result;
	}

	// -------------------------------------------------------------------------
	private void AddHint(LinearLayout layout) {
		UiUtils.AddSmallText(layout, null, Gravity.LEFT, null, MainApplication.getContext().getString(R.string.colorSlotSaveLongTapHint));
	}

	// -------------------------------------------------------------------------
	private void AddColorTable(LinearLayout layout) {
		View[][] colorCell = new View[cRowCount][cColumnCount];
		LinearLayout[] layoutRow = new LinearLayout[cRowCount];

		for (int row = 0; row < cRowCount; row++) {
			LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(UiUtils.dpToPixel(cCellWidth), UiUtils.dpToPixel(cCellWidth));
			layoutRow[row] = new LinearLayout(Context);

			layout.addView(layoutRow[row]);
			layoutRow[row].setOrientation(LinearLayout.HORIZONTAL);
			for (int col = 0; col < cColumnCount; col++) {
				colorCell[row][col] = new LinearLayout(layout.getContext());
				View cell = colorCell[row][col];
				layoutRow[row].addView(cell, lp1);
				lp1.setMargins(cPad, cPad, cPad, cPad);
				SetViewColor(cell, GetCellColor(row, col));
				cell.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						msbRed.setProgress(android.graphics.Color.red((Integer) v.getTag()));
						msbGreen.setProgress(android.graphics.Color.green((Integer) v.getTag()));
						msbBlue.setProgress(android.graphics.Color.blue((Integer) v.getTag()));
						msbTransparency.setProgress(android.graphics.Color.alpha((Integer) v.getTag()));
					}
				});
				final int finalRow = row;
				final int finalCol = col;
				cell.setOnLongClickListener(new View.OnLongClickListener() {
					public boolean onLongClick(View v) {
						SetViewColor(v, GetCurrentColor() );
						PrefUtils.putString( GetSlotKey(finalRow, finalCol), ColorPreference.ToHex( GetCurrentColor() ));
						Toast.makeText(MainApplication.getContext(), R.string.colorSlotSaved, Toast.LENGTH_LONG).show();
						return true;
					}
				});
			}
		}
	}

	// -------------------------------------------------------------------------
	private void AddTextBackgroundSwitch(LinearLayout layout) {
		RadioGroup group = new RadioGroup(Context);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT,
				RadioGroup.LayoutParams.WRAP_CONTENT);

		layout.addView(group, lp);
		group.setOrientation(RadioGroup.HORIZONTAL);
		group.setGravity(Gravity.CENTER);
		if (!IsText || !IsBackground) {
			group.setVisibility(View.GONE);
		}

		RadioButton rbTextColor = new RadioButton(Context);
		rbTextColor.setText(R.string.text);
		rbTextColor.setId(cTextColorMode);
		rbTextColor.setTextColor(Theme.GetMenuFontColor());
		rbTextColor.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
		// rbTextColor.setPadding(0, 10, 0, 0);
		if (!IsText) {
			rbTextColor.setVisibility(View.GONE);
		}
		group.addView(rbTextColor, 0, lp);

		RadioButton rbBackGroundColor = new RadioButton(Context);
		group.addView(rbBackGroundColor, 1, lp);
		rbBackGroundColor.setText(R.string.background1);
		rbBackGroundColor.setId(cBackgroundColorMode);
		rbBackGroundColor.setTextColor(Theme.GetMenuFontColor());
		rbBackGroundColor.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
		if (!IsBackground) {
			rbBackGroundColor.setVisibility(View.GONE);
		}

		group.check(cTextColorMode);
		group.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				ColorMode = checkedId;
				UpdateBars();
			}
		});
	}

	// -------------------------------------------------------------------------
	private void UpdateBars() {
		IsOnProgressChangedEnabled = false;
		if (ColorMode == cTextColorMode) {
			msbRed.setProgress(android.graphics.Color.red((int) mColor.Text));
			msbGreen.setProgress(android.graphics.Color.green((int) mColor.Text));
			msbBlue.setProgress(android.graphics.Color.blue((int) mColor.Text));
			msbTransparency.setProgress(android.graphics.Color.alpha((int) mColor.Text));
		} else {
			msbRed.setProgress(android.graphics.Color.red((int) mColor.Background));
			msbGreen.setProgress(android.graphics.Color.green((int) mColor.Background));
			msbBlue.setProgress(android.graphics.Color.blue((int) mColor.Background));
			msbTransparency.setProgress(android.graphics.Color.alpha((int) mColor.Background));
		}
		IsOnProgressChangedEnabled = true;
		UpdateView();
	}


	// -------------------------------------------------------------------------
	@SuppressLint("DefaultLocale")
	private String GetSlotKey(int row, int col) {
		return String.format("%s_%d_%d", cKey, row, col);
	}

	// -------------------------------------------------------------------------
	private void SetViewColor(View view, int color) {
		view.setBackgroundColor(color);
		view.setTag((Integer) color);
	}

	//-------------------------------------------------------------------------
	private int GetCellColor(int row, int col) {

		int defaultColorInt = Color.parseColor( cDefaultColor );
		int result = defaultColorInt;
		try {
			result = Color.parseColor( PrefUtils.getString(GetSlotKey(row, col), cDefaultColor) );
		} catch (ClassCastException e) {

		}
		if (result == defaultColorInt ) {
			//result = android.graphics.mColor.BLACK;
			switch (row) {
			case 0:
				switch (col) {
				case 0:
					result = Color.BLACK;
					break;
				case 1:
					result = Color.BLUE;
					break;
				case 2:
					result = Color.CYAN;
					break;
				case 3:
					result = Color.DKGRAY;
					break;
				case 4:
					result = Color.GRAY;
					break;
				case 5:
					result = Color.GREEN;
					break;
				case 6:
					result = Color.LTGRAY;
					break;
				case 7:
					result = Color.MAGENTA;
					break;
				}
				;
				break;
			case 1:
				switch (col) {
				case 0:
					result = Color.WHITE;
					break;
				case 1:
					result = Color.YELLOW;
					break;
				case 2:
					result = Color.RED;
					break;
				case 3:
					result = Color.rgb(153, 51, 51);
					break;
				case 4:
					result = Color.rgb(204, 0, 204);
					break;
				case 5:
					result = Color.rgb(153, 204, 255);
					break;
				case 6:
					result = Color.rgb(153, 255, 153);
					break;
				case 7:
					result = Color.rgb(255, 204, 51);
					break;
				}
				;
				break;
			}
		}
		return (int) result;
	}

	// -------------------------------------------------------------------------
    private void AddColorSeekBar(int colorTextResID, LinearLayout layout, TextView labelColor, SeekBar sb) {
		IsOnProgressChangedEnabled = false;
		labelColor.setText(colorTextResID);
		labelColor.setTextColor(Theme.GetMenuFontColor());
		labelColor.setPadding(10, 0, 0, 0);
		layout.addView(labelColor);
		sb.setOnSeekBarChangeListener(this);
		sb.setMax(cMaxColor);
		layout.addView(sb);
		IsOnProgressChangedEnabled = true;
	}

	// -------------------------------------------------------------------------
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (IsOnProgressChangedEnabled) {
			if (ColorMode == cTextColorMode) {
				mColor.Text = GetCurrentColor() ;
			} else {
				mColor.Background = GetCurrentColor();
			}
			UpdateView();
		}
	}

	// -------------------------------------------------------------------------
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	// -------------------------------------------------------------------------
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

	// -------------------------------------------------------------------------
    private void UpdateView() {
		String s;

		s = MainApplication.getContext().getString(R.string.red);
		mlabelRed.setText(String.format("%s: %d", s, msbRed.getProgress()));

		s = MainApplication.getContext().getString(R.string.green);
		mlabelGreen.setText(String.format("%s: %d", s, msbGreen.getProgress()));

		s = MainApplication.getContext().getString(R.string.blue);
		mlabelBlue.setText(String.format("%s: %d", s, msbBlue.getProgress()));

		s = MainApplication.getContext().getString(R.string.transparency);
		mlabelTransparency.setText(String.format("%s: %d", s, msbTransparency.getProgress()));

		if (IsBackground) {
			mviewDialogColor.setTextColor(mColor.Text);
			mviewDialogColor.setBackgroundColor(mColor.Background);
		} else {
			mviewDialogColor.setBackgroundColor(mColor.Text);
		}
	}

	// -------------------------------------------------------------------------
	private int GetCurrentColor() {
		return Color.argb( msbTransparency.getProgress(), msbRed.getProgress(), msbGreen.getProgress(), msbBlue.getProgress() );
	}
}