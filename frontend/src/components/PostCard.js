// src/components/PostCard.jsx
import React, { useState, useMemo } from 'react';
import './PostCard.css';
import { useAuth } from '../context/AuthContext';
import { FiBookmark } from 'react-icons/fi';
import { FaYoutube, FaReddit } from 'react-icons/fa';

function PostCard({ post }) {
    const { isAuthenticated, bookmarkedPostIds, addBookmark, removeBookmark } = useAuth();

    const {
        id,
        title = "Untitled Post",
        description = "",
        author = "Unknown",
        link = "#",
        mediaUrl = null,
        platform = "unknown",
        timestamp = null,
        videoId = null
    } = post || {};

    const [showVideo, setShowVideo] = useState(false);
    const formattedDate = timestamp ? new Date(timestamp).toLocaleString() : 'N/A';

    const isBookmarked = useMemo(() => {
        return isAuthenticated && id && bookmarkedPostIds?.has(id);
    }, [isAuthenticated, bookmarkedPostIds, id]);

    const handlePlayClick = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setShowVideo(true);
    };

    const handleBookmarkClick = async (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (!isAuthenticated || !id) return;

        if (isBookmarked) {
            await removeBookmark(id);
        } else {
            await addBookmark(id);
        }
    };

    const youtubeEmbedUrl = videoId
        ? `https://www.youtube.com/embed/${videoId}?autoplay=1&modestbranding=1&rel=0`
        : null;

    return (
        <div className={`post-card platform-${platform}`}>
            {/* Media Area */}
            <div className="post-media-area">
                {platform === 'youtube' && youtubeEmbedUrl && (
                    <>
                        {!showVideo ? (
                            <div className="youtube-thumbnail-container" onClick={handlePlayClick} title={`Play video: ${title}`}>
                                {mediaUrl ? (
                                    <img src={mediaUrl} alt={`${title} thumbnail`} className="post-thumbnail" loading="lazy" />
                                ) : (
                                    <div className="thumbnail-placeholder">No Thumbnail</div>
                                )}
                                <div className="play-button-overlay">
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="48px" height="48px">
                                        <path d="M8 5v14l11-7z" />
                                    </svg>
                                </div>
                            </div>
                        ) : (
                            <iframe
                                src={youtubeEmbedUrl}
                                title={title}
                                frameBorder="0"
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                                allowFullScreen
                                loading="lazy"
                                className="youtube-iframe"
                            ></iframe>
                        )}
                    </>
                )}

                {platform === 'reddit' && mediaUrl?.startsWith('http') && (
                    <a href={link} target="_blank" rel="noopener noreferrer" className="post-thumbnail-link" title="View original post on Reddit">
                        <img src={mediaUrl} alt={`${title} thumbnail`} className="post-thumbnail reddit-thumbnail" loading="lazy" />
                    </a>
                )}
            </div>

            {/* Content Area */}
            <div className="post-content">
                <div className="post-header">
                    <h4 className="post-title">
                        <a href={link} target="_blank" rel="noopener noreferrer" title={`View original on ${platform}`}>
                            {title}
                        </a>
                    </h4>
                    <span className={`platform-badge platform-${platform}`}>{platform}</span>
                </div>

                {description && <p className="post-description">{description}</p>}

                <div className="post-footer">
                    <span className="post-author">By: {author}</span>
                    <span className="post-timestamp">{formattedDate}</span>
                </div>

                {/* Bookmark and Platform Icon Area */}
                <div className="post-bottom-icons">
                    {isAuthenticated && id && (
                        <button
                            className={`bookmark-button ${isBookmarked ? 'bookmarked' : ''}`}
                            onClick={handleBookmarkClick}
                            title={isBookmarked ? "Remove bookmark" : "Add bookmark"}
                            aria-label={isBookmarked ? "Remove bookmark" : "Add bookmark"}
                        >
                            <FiBookmark />
                        </button>
                    )}

                    {/* Platform Logos */}
                    {platform === 'youtube' && (
                        <FaYoutube className="platform-icon youtube-icon" title="YouTube" />
                    )}
                    {platform === 'reddit' && (
                        <FaReddit className="platform-icon reddit-icon" title="Reddit" />
                    )}
                </div>
            </div>
        </div>
    );
}

export default PostCard;
