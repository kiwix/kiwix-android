package org.kiwix.kiwixmobile.new_bookmarks.entities;

/**
 * Created by EladKeyshawn on 04/04/2017.
 */

public class ReadinglistFolder {
    private String folderTitle; // must be unique
    private int articlesCount;

    public ReadinglistFolder(String folderTitle) {
        this.folderTitle = folderTitle;
        this.articlesCount = 0;
    }

    public void setFolderTitle(String folderTitle) {
        this.folderTitle = folderTitle;
    }

    public void setArticlesCount(int articlesCount) {
        this.articlesCount = articlesCount;
    }

    public String getFolderTitle() {
        return folderTitle;
    }

    public int getArticlesCount() {
        return articlesCount;
    }
}
