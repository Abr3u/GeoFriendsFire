package pt.utl.ist.meic.geofriendsfire.models;

import android.support.v4.app.Fragment;

import java.io.Serializable;

public class PagerItem implements Serializable{
    private String mTitle;
    private Fragment mFragment;


    public PagerItem(String mTitle, Fragment mFragment) {
        this.mTitle = mTitle;
        this.mFragment = mFragment;
    }
    public String getTitle() {
        return mTitle;
    }
    public Fragment getFragment() {
        return mFragment;
    }
    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public void setFragment(Fragment mFragment) {
        this.mFragment = mFragment;
    }

}