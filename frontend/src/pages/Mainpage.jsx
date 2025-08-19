// src/pages/MainPage.jsx
import React, { useState, useEffect, useCallback } from "react";
import { useAuth } from "../context/AuthContext";
import FeedDisplay from "../components/FeedDisplay";
import InterestSelector from "../components/InterestSelector";
import SearchBar from "../components/SearchBar";
import * as api from "../services/api";
import style from './Mainpage.module.css';

const MainPage = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const [posts, setPosts] = useState([]);
  const [isLoadingFeed, setIsLoadingFeed] = useState(false);
  const [feedError, setFeedError] = useState(null);
  const [searchPlatform, setSearchPlatform] = useState(null);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [selectedInterests, setSelectedInterests] = useState([]);

  useEffect(() => {
    const fetchData = async () => {
      setIsLoadingFeed(true);
      setFeedError(null);
      try {
        const params = {
          platform: searchPlatform,
          keyword: searchKeyword,
          interests: selectedInterests.join(","),
        };
        Object.keys(params).forEach((key) => {
          if (!params[key]) delete params[key];
        });

        const feedData = await api.getFeed(params);
        setPosts(feedData || []);
      } catch (err) {
        setFeedError(err.message || "Failed to load feed. Please try again.");
        setPosts([]);
      } finally {
        setIsLoadingFeed(false);
      }
    };

    if (isAuthenticated) {
      fetchData();
    } else {
      setPosts([]);
      setIsLoadingFeed(false);
      setFeedError(null);
    }
  }, [searchPlatform, searchKeyword, selectedInterests, isAuthenticated]);

  const handleSearch = useCallback((platform, keyword) => {
    setSearchPlatform(platform);
    setSearchKeyword(keyword);
  }, []);

  const handleInterestsChange = useCallback((newInterests) => {
    setSelectedInterests(newInterests);
  }, []);

  const handleBookmarks = () => {
    // Navigate to bookmarks page or open modal
    // Example: useNavigate("/bookmarks") if using React Router
    alert("Bookmarks feature coming soon!"); // placeholder
  };

  return (
    <div className="main-page-container p-4 max-w-7xl mx-auto">
      {/* --- Header Section --- */}
      <header className={style["custom-header"]}>
        <h1 className={style["custom-title"]}>FeedFusion</h1>

        <div className={style["header-right"]}>
          {user && (
            <span className={style["welcome-msg"]}>
              Welcome, {user.username || user.email}!
            </span>
          )}
          <button onClick={handleBookmarks} className={style["bookmark-btn"]}>
            My Bookmarks
          </button>
          <button onClick={logout} className={style["logout-btn"]}>
            Logout
          </button>
        </div>
      </header>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <aside className="md:col-span-1 space-y-4">
          <InterestSelector
            selected={selectedInterests}
            onInterestsChange={handleInterestsChange}
          />
        </aside>

        <main className="md:col-span-3 space-y-4">
          <SearchBar onSearch={handleSearch} />
          {isAuthenticated ? (
            <FeedDisplay
              posts={posts}
              isLoading={isLoadingFeed}
              error={feedError}
            />
          ) : (
            <div className="p-4 text-center text-gray-500 border rounded-lg bg-gray-50">
              Please log in to view your feed.
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default MainPage;

