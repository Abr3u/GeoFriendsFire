package pt.utl.ist.meic.geofriendsfire.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.nakama.arraypageradapter.ArrayFragmentStatePagerAdapter;

import java.io.Serializable;
import java.util.ArrayList;

import pt.utl.ist.meic.geofriendsfire.models.PagerItem;

public class DynamicViewPagerAdapter extends ArrayFragmentStatePagerAdapter<PagerItem> implements Serializable {

    public DynamicViewPagerAdapter(FragmentManager fm, ArrayList<PagerItem> datas) {
        super(fm,datas);
    }

    @Override
    public Fragment getFragment(PagerItem item, int position) {
        return item.getFragment();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getItem(position).getTitle();
    }
}
