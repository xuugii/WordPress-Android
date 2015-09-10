package org.wordpress.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import org.wordpress.android.TestUtils;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;

public class CommentTableTest extends InstrumentationTestCase {
    protected Context mTargetContext;
    protected Context mTestContext;

    @Override
    protected void setUp() throws Exception {
        // Clean application state
        mTargetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        mTestContext = getInstrumentation().getContext();
        TestUtils.clearApplicationState(mTargetContext);
        TestUtils.resetEventBus();
    }

    public void testGetCommentEqualTo1024K() {
        createAndGetComment(1024 * 1024);
    }

    public void testGetCommentEqualTo2096550() {
        createAndGetComment(2096550);  // 1024 * 1024 * 2 - 603
    }

    public void testGetCommentEqualTo2096549() {
        createAndGetComment(2096549); // 1024 * 1024 * 2 - 602
    }

    public void testGetCommentEqualTo2048K() {
        createAndGetComment(1024 * 1024 * 2);
    }

    private void createAndGetComment(int commentLength) {
        // Load a sample DB and inject it into WordPress.wpdb
        TestUtils.loadDBFromDump(mTargetContext, mTestContext, "taliwutt-blogs-sample.sql");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < commentLength; ++i) {
            sb.append('a');
        }
        Comment bigComment = new Comment(0,
                1,
                "author",
                "0",
                sb.toString(),
                "approve",
                "arst",
                "http://mop.com",
                "mop@mop.com",
                "");
        CommentTable.addComment(0, bigComment);
        CommentTable.getCommentsForBlog(0);
    }

    private void forceInsertBigComment(SQLiteDatabase db) {
        // Create a huge comment text
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1024 * 1024 * 10; ++i) {
            sb.append('a');
        }

        // Add the comment
        ContentValues values = new ContentValues();
        values.put("blog_id", 0);
        values.put("post_id", 0);
        values.put("comment_id", 1);
        values.put("author_name", "");
        values.put("author_url", "");
        values.put("comment", sb.toString());
        values.put("status", "");
        values.put("author_email", "");
        values.put("post_title", "");
        values.put("published", "");
        values.put("profile_image_url", "");

        db.insertWithOnConflict(CommentTable.COMMENTS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void testCreateAndGetBigComment() {
        // Load a sample DB and inject it into WordPress.wpdb
        SQLiteDatabase db = TestUtils.loadDBFromDump(mTargetContext, mTestContext, "taliwutt-blogs-sample.sql");

        // Force insertion of a big comment
        forceInsertBigComment(db);

        // Try to get that commment, should crash:
        try {
            CommentList list = CommentTable.getCommentsForBlog(0);
            list.size();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Couldn't read row 0, col 6 from CursorWindow"));
            return;
        }

        // Shouldn't go there
        assertTrue(false);
    }

    public void testCreateAndDeleteBigComment() {
        SQLiteDatabase db = TestUtils.loadDBFromDump(mTargetContext, mTestContext, "taliwutt-blogs-sample.sql");

        // Force insertion of a big comment
        forceInsertBigComment(db);

        // deleteBigComments should delete the only (too big) comment
        CommentTable.deleteBigComments(db);
        CommentList list = CommentTable.getCommentsForBlog(0);
        assertEquals(0, list.size());
    }
}
