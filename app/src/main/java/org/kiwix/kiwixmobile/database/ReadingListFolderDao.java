package org.kiwix.kiwixmobile.database;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.kiwix.kiwixmobile.database.entity.Bookmarks;
import org.kiwix.kiwixmobile.database.entity.ReadingListFolders;
import org.kiwix.kiwixmobile.new_bookmarks.entities.BookmarkArticle;
import org.kiwix.kiwixmobile.new_bookmarks.entities.ReadinglistFolder;

import java.util.ArrayList;

/**
 * Created by EladKeyshawn on 04/04/2017.
 */

public class ReadingListFolderDao {
    private KiwixDatabase mDb;


    public ReadingListFolderDao(KiwixDatabase kiwikDatabase) {
        this.mDb = kiwikDatabase;
    }





    public ArrayList<BookmarkArticle> getArticlesOfFolder(ReadinglistFolder folder) {
        SquidCursor<Bookmarks> articlesInFolderCursor = mDb.query(
                Bookmarks.class,
                Query.selectDistinct(Bookmarks.BOOKMARK_TITLE).where(Bookmarks.PARENT_READINGLIST.eq(folder.getFolderTitle()))
                        .orderBy(Bookmarks.BOOKMARK_TITLE.asc()));
        ArrayList<BookmarkArticle> result = new ArrayList<>();
        try {
            while (articlesInFolderCursor.moveToNext()) {
                BookmarkArticle bookmark = new BookmarkArticle();
                bookmark.setBookmarkUrl(articlesInFolderCursor.get(Bookmarks.BOOKMARK_URL));
                bookmark.setBookmarkTitle(articlesInFolderCursor.get(Bookmarks.BOOKMARK_TITLE));
                bookmark.setParentReadinglist(articlesInFolderCursor.get(Bookmarks.PARENT_READINGLIST));
                result.add(bookmark);
            }

        } finally {
            articlesInFolderCursor.close();
        }
        return result;
    }

    public ArrayList<ReadinglistFolder> getFolders() {
        SquidCursor<ReadingListFolders> foldersCursor = mDb.query(
                ReadingListFolders.class,
                Query.selectDistinct(ReadingListFolders.FOLDER_TITLE)
                        .orderBy(ReadingListFolders.FOLDER_TITLE.asc()));
        ArrayList<ReadinglistFolder> result = new ArrayList<>();
        try {
            while (foldersCursor.moveToNext()) {
                result.add(new ReadinglistFolder(foldersCursor.get(ReadingListFolders.FOLDER_TITLE)));
            }
        } finally {
            foldersCursor.close();
        }
        return result;
    }


    public void saveFolder(ReadinglistFolder folder) {
        if (folder != null) {
            mDb.persist(new ReadingListFolders().setFolderTitle(folder.getFolderTitle()).setArticleCount(folder.getArticlesCount()));
        }

    }


    public void saveBookmark(BookmarkArticle article) {
        if (article != null) {
            mDb.persist(new Bookmarks().setBookmarkTitle(article.getBookmarkTitle())
                    .setBookmarkUrl(article.getBookmarkUrl())
                    .setParentReadinglist(article.getParentReadinglist())
                    .setZimId(article.getZimId())
                    .setZimName(article.getZimName()));

        }
    }

    public void deleteFolder(String folderName) {
        //TODO: when folder is deleted all articles saved in folder should be deleted
        mDb.deleteWhere(Bookmarks.class, Bookmarks.PARENT_READINGLIST.eq(folderName));
        mDb.deleteWhere(ReadingListFolders.class, ReadingListFolders.FOLDER_TITLE.eq(folderName));
    }


    public void deleteAll(){
        mDb.clear();
    }

}


