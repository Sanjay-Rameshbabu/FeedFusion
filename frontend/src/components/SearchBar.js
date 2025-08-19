import React, { useState } from 'react';

function SearchBar({ onSearch }) {
    const [platform, setPlatform] = useState('all'); // Default to 'all'
    const [keyword, setKeyword] = useState('');

    const handleSearch = (e) => {
        e.preventDefault(); // Prevent form submission reload
        onSearch(platform === 'all' ? null : platform, keyword.trim());
    };

    return (
        <form onSubmit={handleSearch} className="search-bar">
            <select value={platform} onChange={(e) => setPlatform(e.target.value)}>
                <option value="all">All Platforms</option>
                <option value="reddit">Reddit</option>
                <option value="youtube">YouTube</option>
            </select>
            <input
                type="text"
                placeholder="Search by keyword..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
            />
            <button type="submit">Search / Filter</button>
        </form>
    );
}

export default SearchBar;