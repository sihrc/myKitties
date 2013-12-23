package com.sihrc.kitty;




import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

/**
 * Created by chris on 12/22/13.
 */
public class AdapterFragmentCollection extends FragmentStatePagerAdapter {
    //Fragment ArrayList
    ArrayList<Fragment> fragments = new ArrayList<Fragment>();

    //Default Constructor
    public AdapterFragmentCollection(FragmentManager fragMan){
        super(fragMan);
        createFragments();
    }

    private void createFragments(){
        fragments.add(new Fragment());
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Fragment getItem(int i) {
        return null;
    }


}