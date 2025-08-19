// // src/context/AuthContext.js
// import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';
// import { useNavigate } from 'react-router-dom';
// // Import your API functions (ensure api.js exports login, signup, setAuthToken)
// import * as api from '../services/api';

// // Create the context
// const AuthContext = createContext(null);

// // AuthProvider component
// export const AuthProvider = ({ children }) => {
//     const [user, setUser] = useState(null); // Holds user info after login
//     const [token, setToken] = useState(() => localStorage.getItem('authToken')); // Initialize token from storage
//     const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('authToken')); // Initial auth state
//     const [isLoading, setIsLoading] = useState(false); // Loading state for auth operations
//     const [error, setError] = useState(null); // Error state for auth operations
//     const navigate = useNavigate();

//     // Effect to set token in API headers and update auth state when token changes
//     useEffect(() => {
//         if (token) {
//             localStorage.setItem('authToken', token);
//             api.setAuthToken(token); // Set token for future API calls
//             setIsAuthenticated(true);
//             // Optional: Fetch user details using the token if not returned on login
//             // api.getCurrentUser().then(setUser).catch(logout);
//         } else {
//             localStorage.removeItem('authToken');
//             api.setAuthToken(null); // Clear token from API calls
//             setIsAuthenticated(false);
//             setUser(null);
//         }
//     }, [token]);

//     // Login function
//     const login = useCallback(async (credentials) => {
//         setIsLoading(true);
//         setError(null);
//         try {
//             // Call the API login function (adjust endpoint/payload if needed)
//             // Assumes api.login sends { username, password } or { email, password }
//             // Assumes backend responds with { token: '...', user: {...} } or similar
//             const response = await api.login(credentials);

//             if (response && response.token) {
//                 setToken(response.token); // Update token state (triggers useEffect)
//                 setUser(response.user || null); // Set user state if provided
//                 navigate('/'); // Redirect to main page on successful login
//                 setIsLoading(false);
//                 return true;
//             } else {
//                 throw new Error(response?.message || 'Login failed: No token received.');
//             }
//         } catch (err) {
//             console.error("Login error:", err);
//             setError(err.message || 'Login failed. Please check credentials.');
//             setToken(null); // Clear token on failure
//             setIsLoading(false);
//             return false;
//         }
//     }, [navigate]); // Dependency: navigate

//     // Signup function
//     const signup = useCallback(async (userData) => {
//         setIsLoading(true);
//         setError(null);
//         try {
//             // Call the API signup function (adjust endpoint/payload if needed)
//             // Assumes api.signup sends { username, email, password } or similar
//             // Assumes backend responds with success message or maybe auto-login token
//             const response = await api.signup(userData);

//             // Handle different backend responses:
//             if (response && response.success) { // Simple success message
//                  setIsLoading(false);
//                  // Optional: Show success message to user before redirecting
//                  navigate('/login'); // Redirect to login page after signup
//                  return true;
//             } else if (response && response.token) { // Auto-login after signup
//                  setToken(response.token);
//                  setUser(response.user || null);
//                  navigate('/'); // Redirect to main page
//                  setIsLoading(false);
//                  return true;
//             }
//             else {
//                 throw new Error(response?.message || 'Signup failed: Unknown response.');
//             }
//         } catch (err) {
//             console.error("Signup error:", err);
//             setError(err.message || 'Signup failed. Please try again.');
//             setIsLoading(false);
//             return false;
//         }
//     }, [navigate]); // Dependency: navigate

//     // Logout function
//     const logout = useCallback(() => {
//         setToken(null); // Clear token state (triggers useEffect)
//         navigate('/login'); // Redirect to login page
//     }, [navigate]); // Dependency: navigate

//     // Value provided by the context
//     const value = {
//         isAuthenticated,
//         user,
//         token,
//         isLoading,
//         error, // Provide error state to components
//         login,
//         signup,
//         logout,
//     };

//     return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
// };

// // Custom hook to easily consume the context
// export const useAuth = () => {
//     const context = useContext(AuthContext);
//     if (context === undefined) { // Ensure it's used within the Provider
//         throw new Error('useAuth must be used within an AuthProvider');
//     }
//     return context;
// };
// src/context/AuthContext.js
import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import * as api from '../services/api'; // Ensure this path is correct and api.js exports bookmark functions

const AuthContext = createContext(null);
const AUTH_TOKEN_KEY = 'authToken';

export const AuthProvider = ({ children }) => {
    console.log("AuthProvider rendering...");

    const [token, setToken] = useState(() => localStorage.getItem(AUTH_TOKEN_KEY));
    const [isAuthenticated, setIsAuthenticated] = useState(() => !!localStorage.getItem(AUTH_TOKEN_KEY));
    const [user, setUser] = useState(null);
    const [isLoading, setIsLoading] = useState(false); // Loading for login/signup
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    // --- State for Bookmarks ---
    const [bookmarkedPostIds, setBookmarkedPostIds] = useState(new Set()); // Use a Set for efficient lookups
    const [isLoadingBookmarks, setIsLoadingBookmarks] = useState(false);
    // --- End State for Bookmarks ---

    // --- Function to Fetch Bookmark IDs ---
    const fetchBookmarkIds = useCallback(async () => {
        if (!isAuthenticated || !token) { // Check for token as well
             console.log("AuthContext: Skipping bookmark ID fetch: User not authenticated or no token.");
             setBookmarkedPostIds(new Set());
             return;
        }
        console.log("AuthContext: Fetching bookmark IDs...");
        setIsLoadingBookmarks(true);
        try {
            // Ensure api.getBookmarkIds is exported from api.js
            const idsSet = await api.getBookmarkIds();
            setBookmarkedPostIds(idsSet);
            console.log("AuthContext: Bookmark IDs fetched successfully:", idsSet);
        } catch (fetchError) {
            console.error("AuthContext: Failed to fetch bookmark IDs:", fetchError);
            setBookmarkedPostIds(new Set());
        } finally {
            setIsLoadingBookmarks(false);
        }
    }, [isAuthenticated, token]); // Depend on authentication state and token

    // --- Effect to fetch bookmarks when authentication state (via token) changes ---
    useEffect(() => {
        if (isAuthenticated && token) { // Only fetch if truly authenticated and token exists
            fetchBookmarkIds();
        } else {
            setBookmarkedPostIds(new Set()); // Clear if not authenticated
        }
    }, [isAuthenticated, token, fetchBookmarkIds]); // Added fetchBookmarkIds to dependencies


    // Effect for token synchronization
    useEffect(() => {
        console.log("AuthContext useEffect[token] running. Current token:", token);
        if (token) {
            if (localStorage.getItem(AUTH_TOKEN_KEY) !== token) {
                localStorage.setItem(AUTH_TOKEN_KEY, token);
            }
            api.setAuthToken(token);
            setIsAuthenticated(currentAuth => !currentAuth ? true : currentAuth);
            // Consider fetching user details here if not already present
            // if (!user) { /* fetch user details */ }
        } else {
            if (localStorage.getItem(AUTH_TOKEN_KEY) !== null) {
                localStorage.removeItem(AUTH_TOKEN_KEY);
            }
            api.setAuthToken(null);
            setIsAuthenticated(currentAuth => currentAuth ? false : currentAuth);
            setUser(null);
            // setBookmarkedPostIds(new Set()); // Already handled by the effect above
        }
    }, [token]);

    // --- Bookmark Action Functions ---
    const addBookmark = useCallback(async (postId) => {
        if (!isAuthenticated || !postId) {
            console.warn("AuthContext: Add bookmark skipped (not authenticated or no postId).");
            return false;
        }
        console.log(`AuthContext: Adding bookmark ${postId}`);
        // Optimistic UI update (optional)
        // setBookmarkedPostIds(prev => new Set(prev).add(postId));
        try {
            // Ensure api.addBookmark is exported from api.js
            await api.addBookmark(postId);
            setBookmarkedPostIds(prev => new Set(prev).add(postId)); // Update after success
            return true;
        } catch (err) {
            console.error(`AuthContext: Failed to add bookmark ${postId}:`, err);
            // Revert optimistic update if needed
            setError("Failed to add bookmark.");
            return false;
        }
    }, [isAuthenticated]); // Dependency on isAuthenticated

    const removeBookmark = useCallback(async (postId) => {
        if (!isAuthenticated || !postId) {
            console.warn("AuthContext: Remove bookmark skipped (not authenticated or no postId).");
            return false;
        }
        console.log(`AuthContext: Removing bookmark ${postId}`);
        // Optimistic UI update (optional)
        // setBookmarkedPostIds(prev => { const newSet = new Set(prev); newSet.delete(postId); return newSet; });
        try {
            // Ensure api.removeBookmark is exported from api.js
            await api.removeBookmark(postId);
            setBookmarkedPostIds(prev => { // Update after success
                const newSet = new Set(prev);
                newSet.delete(postId);
                return newSet;
            });
            return true;
        } catch (err) {
            console.error(`AuthContext: Failed to remove bookmark ${postId}:`, err);
            // Revert optimistic update if needed
            setError("Failed to remove bookmark.");
            return false;
        }
    }, [isAuthenticated]); // Dependency on isAuthenticated

    // Login function
    const login = useCallback(async (credentials) => {
        console.log("AuthContext login called.");
        setIsLoading(true);
        setError(null);
        try {
            const response = await api.login(credentials);
            if (response && response.token) {
                console.log("AuthContext login success, setting token.");
                setUser(response.user || null);
                setToken(response.token); // Triggers useEffect[token] which handles isAuthenticated & fetches bookmarks
                navigate('/');
                return true;
            } else { throw new Error(response?.message || 'Login failed: No token received.'); }
        } catch (err) {
             console.error("AuthContext login error:", err);
             setError(err.message || 'Login failed.');
             setToken(null); // Triggers useEffect[token]
             return false;
        } finally {
             setIsLoading(false);
        }
    }, [navigate]);

    // Signup function
    const signup = useCallback(async (userData) => {
         console.log("AuthContext signup called.");
         setIsLoading(true);
         setError(null);
         try {
             const response = await api.signup(userData);
             if (response && response.success) {
                  console.log("AuthContext signup success (no auto-login), navigating to login.");
                  navigate('/login');
                  return true;
             } else if (response && response.token) { // Auto-login
                  console.log("AuthContext signup success (auto-login), setting token.");
                  setUser(response.user || null);
                  setToken(response.token); // Triggers useEffect[token]
                  navigate('/');
                  return true;
             } else { throw new Error(response?.message || 'Signup failed.'); }
         } catch (err) {
              console.error("AuthContext signup error:", err);
              setError(err.message || 'Signup failed.');
              return false;
         } finally {
              setIsLoading(false);
         }
    }, [navigate]);

    // Logout function
    const logout = useCallback(() => {
        console.log("AuthContext logout called.");
        setToken(null); // Triggers useEffect[token] which clears auth state and bookmarks
        // navigate('/login'); // Navigation is handled by ProtectedRoute when isAuthenticated changes
    }, []); // Removed navigate dependency as it's not directly used for navigation here


    // Updated context value
    const value = {
        isAuthenticated,
        user,
        token,
        isLoading,
        error,
        login,
        signup,
        logout,
        // --- Add bookmark state and functions ---
        bookmarkedPostIds,
        isLoadingBookmarks,
        addBookmark,        // Exported to context consumers
        removeBookmark      // Exported to context consumers
        // --- End bookmark additions ---
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
