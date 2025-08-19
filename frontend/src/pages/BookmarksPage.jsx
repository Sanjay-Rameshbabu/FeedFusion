// src/pages/BookmarksPage.jsx
import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext'; // To check auth
import * as api from '../services/api'; // To call getBookmarkedPosts
import FeedDisplay from '../components/FeedDisplay'; // To display the posts
import { Link } from 'react-router-dom'; // For a "back to feed" link

// Import CSS Module for this page
import styles from './BookmarksPage.module.css'; // Make sure this file exists

const BookmarksPage = () => {
    // Get isAuthenticated to ensure user is logged in before fetching
    const { isAuthenticated } = useAuth();
    const [bookmarkedPosts, setBookmarkedPosts] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null); // To store error messages

    // useCallback to memoize the fetch function
    const fetchUserBookmarkedPosts = useCallback(async () => {
        // Guard clause: only fetch if the user is authenticated
        if (!isAuthenticated) {
            console.log("BookmarksPage: User not authenticated. Skipping fetch.");
            setBookmarkedPosts([]); // Clear any existing posts
            setIsLoading(false);    // Ensure loading is off
            setError(null);         // Clear any errors
            return;
        }

        console.log("BookmarksPage: Fetching bookmarked posts...");
        setIsLoading(true); // Set loading true before the API call
        setError(null);     // Clear any previous errors
        try {
            // Call the API function from api.js to get full bookmarked post objects
            const posts = await api.getBookmarkedPosts();
            setBookmarkedPosts(posts || []); // Ensure it's always an array, even if API returns null/undefined
            console.log("BookmarksPage: Bookmarked posts fetched successfully:", posts);
        } catch (err) {
            console.error("BookmarksPage: Failed to fetch bookmarked posts:", err);
            // Set a user-friendly error message
            setError(err.message || "Failed to load your bookmarks. Please try again later.");
            setBookmarkedPosts([]); // Clear posts on error
        } finally {
            // Always set loading to false after the attempt
            setIsLoading(false);
        }
    }, [isAuthenticated]); // Dependency: re-fetch if authentication status changes

    // useEffect to call fetchUserBookmarkedPosts when the component mounts
    // or when the fetchUserBookmarkedPosts function reference changes (due to its deps changing)
    useEffect(() => {
        fetchUserBookmarkedPosts();
    }, [fetchUserBookmarkedPosts]);

    return (
        <div className={styles.pageContainer}>
            <header className={styles.header}>
                <h1 className={styles.title}>My Bookmarks</h1>
                <Link to="/" className={styles.backLink}>
                    &larr; Back to Main Feed
                </Link>
            </header>

            {/* Conditional rendering based on loading, error, or posts available */}
            {isLoading ? (
                <p className={styles.loadingMessage}>Loading your bookmarks...</p>
            ) : error ? (
                <p className={styles.errorMessage}>Error: {error}</p>
            ) : bookmarkedPosts.length > 0 ? (
                // Use FeedDisplay to render the bookmarked posts
                // Pass relevant props to FeedDisplay
                <FeedDisplay posts={bookmarkedPosts} isLoading={false} error={null} />
            ) : (
                // Message if no bookmarks are found (and not loading/no error)
                <p className={styles.emptyMessage}>You haven't bookmarked any posts yet. Start exploring and save your favorites!</p>
            )}
        </div>
    );
};

export default BookmarksPage;
