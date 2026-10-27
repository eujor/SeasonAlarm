package com.eujor.seasonalarm;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int BG = Color.rgb(244, 248, 248);
    public static final int CARD = Color.WHITE;
    public static final int INK = Color.rgb(18, 38, 43);
    public static final int MUTED = Color.rgb(91, 111, 116);
    public static final int BRAND = Color.rgb(0, 105, 92);
    public static final int BRAND_DARK = Color.rgb(9, 31, 38);
    public static final int BRAND_SOFT = Color.rgb(224, 242, 239);
    public static final int LINE = Color.rgb(218, 228, 229);
    public static final int DANGER = Color.rgb(176, 49, 49);
    public static final int SUCCESS = Color.rgb(28, 123, 79);

    private Ui() {}

    public static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static TextView text(Context c, String value, float sp, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setLineSpacing(0f, 1.08f);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    public static LinearLayout card(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 16));
        card.setBackground(rounded(CARD, 20, LINE, 1, c));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(c, 14));
        card.setLayoutParams(lp);
        return card;
    }

    public static TextView sectionTitle(Context c, String value) {
        TextView v = text(c, value, 13, MUTED, true);
        v.setLetterSpacing(0.08f);
        v.setPadding(dp(c, 2), dp(c, 12), 0, dp(c, 10));
        return v;
    }

    public static Button primaryButton(Context c, String value) {
        Button b = new Button(c);
        b.setText(value);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setMinHeight(dp(c, 50));
        b.setBackground(rounded(BRAND, 16, BRAND, 0, c));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(c, 6), 0, dp(c, 8));
        b.setLayoutParams(lp);
        return b;
    }

    public static Button secondaryButton(Context c, String value) {
        Button b = new Button(c);
        b.setText(value);
        b.setTextColor(INK);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setMinHeight(dp(c, 46));
        b.setBackground(rounded(Color.WHITE, 14, LINE, 1, c));
        return b;
    }

    public static Button tabButton(Context c, String value, boolean selected) {
        Button b = new Button(c);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinHeight(dp(c, 46));
        styleTab(c, b, selected);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(c, 3), 0, dp(c, 3), 0);
        b.setLayoutParams(lp);
        return b;
    }

    public static void styleTab(Context c, Button b, boolean selected) {
        b.setTextColor(selected ? Color.WHITE : INK);
        b.setBackground(rounded(selected ? BRAND : Color.TRANSPARENT, 14,
                selected ? BRAND : LINE, selected ? 0 : 1, c));
    }

    public static Button chip(Context c, String value, boolean selected) {
        Button b = new Button(c);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setMinHeight(dp(c, 40));
        b.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        b.setTextColor(selected ? Color.WHITE : INK);
        b.setBackground(rounded(selected ? BRAND : Color.WHITE, 20,
                selected ? BRAND : LINE, 1, c));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(c, 40));
        lp.setMargins(0, 0, dp(c, 8), 0);
        b.setLayoutParams(lp);
        return b;
    }

    public static TextView badge(Context c, String value, int fg, int bg) {
        TextView v = text(c, value, 11, fg, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c, 10), dp(c, 5), dp(c, 10), dp(c, 5));
        v.setBackground(rounded(bg, 20, bg, 0, c));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(c, 1)));
        return v;
    }

    public static GradientDrawable rounded(int color, int radiusDp, int strokeColor, int strokeDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) g.setStroke(dp(c, strokeDp), strokeColor);
        return g;
    }
}
