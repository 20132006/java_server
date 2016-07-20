package com.sv2x.googlemap3;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by netlab on 7/20/16.
 */
class Spaces {
    List<Space> spaceList = new ArrayList<Space>();
    void addSpace( Space s ) {
        spaceList.add(s);
    }
    void removeSpace( Space s ) {
        spaceList.remove(s);
    }
    public Space findSpace( String id ) {
        for(Space s : spaceList) {
            if(s.spaceId.equals(id)) {
                return s;
            }
        }
        return null;
    }
};