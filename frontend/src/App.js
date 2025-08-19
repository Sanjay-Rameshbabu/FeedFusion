// src/App.js
import React from 'react';
import { Routes, Route, Navigate, Outlet, Link } from 'react-router-dom';
import { useAuth } from './context/AuthContext';

// Import Page Components
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import MainPage from './pages/Mainpage'; // Ensure casing matches your filename
import BookmarksPage from './pages/BookmarksPage';

import './App.css';

// --- Helper Components for Routing ---

const ProtectedRoute = () => {
    // Removed 'isLoading' as it was not used
    const { isAuthenticated } = useAuth();

    return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
};

const PublicRoute = () => {
    // Removed 'isLoading' as it was not used
    const { isAuthenticated } = useAuth();

    return !isAuthenticated ? <Outlet /> : <Navigate to="/" replace />;
};


// --- Main App Component ---
function App() {
    return (
        <div className="App">
            <Routes>
                {/* Public Routes (Login, Signup) */}
                <Route element={<PublicRoute />}>
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/signup" element={<SignupPage />} />
                </Route>

                {/* Protected Routes (Main Application & Bookmarks) */}
                <Route element={<ProtectedRoute />}>
                    <Route path="/" element={<MainPage />} />
                    <Route path="/bookmarks" element={<BookmarksPage />} />
                    {/* Add other protected routes here */}
                </Route>

                {/* Fallback Route */}
                 <Route path="*" element={<div><h2>404 Not Found</h2><Link to="/">Go Home</Link></div>} />
            </Routes>
        </div>
    );
}

export default App;
