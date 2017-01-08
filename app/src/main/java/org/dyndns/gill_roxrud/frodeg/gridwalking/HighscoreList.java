package org.dyndns.gill_roxrud.frodeg.gridwalking;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;


class HighscoreList implements Parcelable {
    private int playerPosition;
    private int totalPlayerCount;
    private ArrayList<HighscoreItem> items;

    public HighscoreList() {
    }

    private HighscoreList(Parcel in) {
        playerPosition = in.readInt();
        totalPlayerCount = in.readInt();
        items = in.createTypedArrayList(HighscoreItem.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(playerPosition);
        dest.writeInt(totalPlayerCount);
        dest.writeTypedList(items);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HighscoreList> CREATOR = new Creator<HighscoreList>() {
        @Override
        public HighscoreList createFromParcel(Parcel in) {
            return new HighscoreList(in);
        }

        @Override
        public HighscoreList[] newArray(int size) {
            return new HighscoreList[size];
        }
    };

    public void setPosition(final String positionString) {
        String[] atoms = positionString.split(";");
        playerPosition = Integer.valueOf(atoms[0]);
        totalPlayerCount = Integer.valueOf(atoms[1]);
    }

    public int getPlayerPosition() {
        return playerPosition;
    }

    public int getTotalPlayerCount() {
        return totalPlayerCount;
    }

    public ArrayList<HighscoreItem> getHighscoreItemList() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }
}
