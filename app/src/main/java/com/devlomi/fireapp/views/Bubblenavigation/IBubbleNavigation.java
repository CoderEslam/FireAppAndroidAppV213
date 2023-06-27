package com.devlomi.fireapp.views.Bubblenavigation;

import android.graphics.Typeface;

import com.devlomi.fireapp.views.Bubblenavigation.listener.BubbleNavigationChangeListener;


@SuppressWarnings("unused")
public interface IBubbleNavigation {
    void setNavigationChangeListener(BubbleNavigationChangeListener navigationChangeListener);

    void setTypeface(Typeface typeface);

    int getCurrentActiveItemPosition();

    void setCurrentActiveItem(int position);

    void setBadgeValue(int position, String value);
}
