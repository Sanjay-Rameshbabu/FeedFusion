// src/services/api.js
import axios from 'axios';

// Define your backend base URL
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

// Create an axios instance
const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Function to set the Authorization token globally for axios requests
// Make sure this is exported so AuthContext can use it
export const setAuthToken = (token) => {
    if (token) {
        apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        console.log("Auth token set in API client");
    } else {
        delete apiClient.defaults.headers.common['Authorization'];
        console.log("Auth token removed from API client");
    }
};

// === Authentication API Calls ===

// Ensure login is exported
export const login = async (credentials) => {
    try {
        const response = await apiClient.post('/auth/login', credentials);
        // Set token immediately after successful login FOR SUBSEQUENT CALLS in this session
        if (response.data && response.data.token) {
            // The AuthContext will call setAuthToken after receiving the token
        }
        return response.data;
    } catch (error) {
        console.error('API Login error:', error.response?.data || error.message);
        throw new Error(error.response?.data?.message || error.message || 'Login request failed');
    }
};

// Ensure signup is exported
export const signup = async (userData) => {
    try {
        const response = await apiClient.post('/auth/register', userData);
        return response.data;
    } catch (error) {
        console.error('API Signup error:', error.response?.data || error.message);
        throw new Error(error.response?.data?.message || error.message || 'Signup request failed');
    }
};

// === Feed API Call ===

// Ensure getFeed is exported
export const getFeed = async (params = {}) => {
     try {
         // This request will include the Authorization header if setAuthToken was called
         console.log("API: Fetching feed with params:", params);
         const response = await apiClient.get('/feed', { params });
         console.log("API: Feed response received:", response.data);
         return response.data; // Assuming backend returns an array of posts
     } catch (error) {
         console.error('API Get Feed error:', error.response?.data || error.message, error.response?.status);
         // Handle specific errors like 401 Unauthorized if needed
         if (error.response?.status === 401) {
             console.warn("API: Unauthorized access to feed. Token might be invalid or missing.");
             // Consider triggering logout or token refresh here if applicable
         }
         // Rethrow a standard error object for consistent handling
         throw new Error(error.response?.data?.message || error.message || 'Failed to fetch feed');
     }
 };


// === Bookmark API Calls ===

/**
 * Fetches the IDs of the posts bookmarked by the current user.
 * Requires authentication.
 * @returns {Promise<Set<String>>} - A promise resolving to a Set of bookmarked post IDs.
 */
// --- V V V Ensure this is exported V V V ---
export const getBookmarkIds = async () => {
    try {
        // Ensure token is set before calling this
        console.log("API: Fetching bookmark IDs");
        const response = await apiClient.get('/bookmarks/ids');
        // Assuming backend returns a list/set of strings
        return new Set(response.data || []); // Convert array to Set, handle null response
    } catch (error) {
        console.error('API Get Bookmark IDs error:', error.response?.data || error.message, error.response?.status);
        if (error.response?.status === 401) {
             console.warn("API: Unauthorized access to bookmark IDs.");
        }
        throw new Error(error.response?.data?.message || error.message || 'Failed to fetch bookmark IDs');
    }
};

/**
 * Adds a post to the current user's bookmarks.
 * Requires authentication.
 * @param {string} postId - The ID (_id) of the FeedPost to bookmark.
 * @returns {Promise<object>} - The response data (e.g., { message: "..." }).
 */
// --- V V V Ensure this is exported V V V ---
export const addBookmark = async (postId) => {
     try {
         console.log(`API: Adding bookmark for post ID: ${postId}`);
         // Backend expects { "postId": "value" } in the body
         const response = await apiClient.post('/bookmarks', { postId });
         return response.data;
     } catch (error) {
         console.error(`API Add Bookmark error for post ${postId}:`, error.response?.data || error.message, error.response?.status);
          if (error.response?.status === 401) {
             console.warn("API: Unauthorized attempt to add bookmark.");
         }
         throw new Error(error.response?.data?.message || error.message || 'Failed to add bookmark');
     }
};

/**
 * Removes a post from the current user's bookmarks.
 * Requires authentication.
 * @param {string} postId - The ID (_id) of the FeedPost to unbookmark.
 * @returns {Promise<object>} - The response data (e.g., { message: "..." }).
 */
// --- V V V Ensure this is exported V V V ---
export const removeBookmark = async (postId) => {
     try {
          console.log(`API: Removing bookmark for post ID: ${postId}`);
         const response = await apiClient.delete(`/bookmarks/${postId}`);
         return response.data;
     } catch (error) {
         console.error(`API Remove Bookmark error for post ${postId}:`, error.response?.data || error.message, error.response?.status);
          if (error.response?.status === 401) {
             console.warn("API: Unauthorized attempt to remove bookmark.");
         }
         throw new Error(error.response?.data?.message || error.message || 'Failed to remove bookmark');
     }
};

/**
 * Fetches the full FeedPost objects for the current user's bookmarks.
 * Requires authentication.
 * @returns {Promise<Array>} - A promise resolving to an array of bookmarked FeedPost objects.
 */
// --- V V V Ensure this is exported V V V ---
export const getBookmarkedPosts = async () => {
    try {
        console.log("API: Fetching full bookmarked posts");
        const response = await apiClient.get('/bookmarks');
        return response.data || []; // Return empty array if data is null/undefined
    } catch (error) {
        console.error('API Get Bookmarked Posts error:', error.response?.data || error.message, error.response?.status);
        if (error.response?.status === 401) {
             console.warn("API: Unauthorized access to bookmarked posts.");
        }
        throw new Error(error.response?.data?.message || error.message || 'Failed to fetch bookmarked posts');
    }
};
