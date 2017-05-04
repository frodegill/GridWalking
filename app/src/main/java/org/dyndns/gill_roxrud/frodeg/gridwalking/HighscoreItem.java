package org.dyndns.gill_roxrud.frodeg.gridwalking;


import android.os.Parcel;
import android.os.Parcelable;


public class HighscoreItem implements Parcelable {
    private int position;
    private String username;
    private int[] levels = new int[Grid.LEVEL_COUNT];
    private long score;

    public HighscoreItem() {
    }

    private HighscoreItem(Parcel in) {
        position = in.readInt();
        username = in.readString();
        levels = in.createIntArray();
        score = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(position);
        dest.writeString(username);
        dest.writeIntArray(levels);
        dest.writeLong(score);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HighscoreItem> CREATOR = new Creator<HighscoreItem>() {
        @Override
        public HighscoreItem createFromParcel(Parcel in) {
            return new HighscoreItem(in);
        }

        @Override
        public HighscoreItem[] newArray(int size) {
            return new HighscoreItem[size];
        }
    };

    public void setItem(final String highscoreLine) {
        String[] atoms = highscoreLine.split(";");
        position = Integer.valueOf(atoms[0]);
        int i;
        for (i=0; i<Grid.LEVEL_COUNT; i++) {
            levels[Grid.LEVEL_COUNT-i-1] = Integer.valueOf(atoms[1+i]);
        }
        score = Long.valueOf(atoms[1+Grid.LEVEL_COUNT]);
        username = atoms[2+Grid.LEVEL_COUNT];
        for (i=3+Grid.LEVEL_COUNT; i<atoms.length; i++) {
            username += ";"+atoms[i];
        }
    }

    public int getPosition() {
        return position;
    }

    public String getUsername() {
        return username;
    }

    public String getLevelsString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        byte i;
        for (i = Grid.LEVEL_COUNT - 1; i >= 0; i--) {
            if (!(levels[i]==0 && first)) {
                if (!first) {
                    sb.append(':');
                }
                sb.append(Integer.toString(levels[i]));
                first = false;
            }
        }
        return sb.toString();
    }

    public long getScore() {
        return score;
    }
}
