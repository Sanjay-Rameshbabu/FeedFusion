import React from 'react';
import PostCard from './PostCard';

function FeedDisplay({ posts, isLoading, error }) {
    if (isLoading) {
        return <p>Loading feed...</p>;
    }

    if (error) {
        return <p style={{ color: 'red' }}>Error loading feed: {error.message || 'Please try again later.'}</p>;
    }

    if (!posts || posts.length === 0) {
        return <p>No posts found. Try selecting different interests or adjusting search filters.</p>;
    }

    return (
        <div className="feed-display">
            {posts.map((post) => (
                // Use post.id (MongoID) or post.link as key if ID is guaranteed unique
                <PostCard key={post.id || post.link} post={post} />
            ))}
        </div>
    );
}

export default FeedDisplay;