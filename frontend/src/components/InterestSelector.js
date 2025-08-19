import React, { useState, useEffect } from 'react';

// Sample list of interests - could be fetched from backend or configured elsewhere
const availableInterests = ['Technology', 'Travel', 'Sports', 'Gaming', 'Science', 'Music', 'Movies', 'Food'];
const LOCAL_STORAGE_KEY = 'feedfusion-interests';

function InterestSelector({ onInterestsChange }) {
    const [selectedInterests, setSelectedInterests] = useState([]);

    // Load interests from LocalStorage on initial mount
    useEffect(() => {
        const storedInterests = localStorage.getItem(LOCAL_STORAGE_KEY);
        if (storedInterests) {
            try {
                const parsedInterests = JSON.parse(storedInterests);
                setSelectedInterests(parsedInterests);
                onInterestsChange(parsedInterests); // Notify parent on load
            } catch (e) {
                console.error("Failed to parse interests from LocalStorage", e);
                localStorage.removeItem(LOCAL_STORAGE_KEY); // Clear invalid data
            }
        }
    }, [onInterestsChange]); // Added onInterestsChange dependency

    const handleCheckboxChange = (event) => {
        const { value, checked } = event.target;
        let updatedInterests;
        if (checked) {
            updatedInterests = [...selectedInterests, value];
        } else {
            updatedInterests = selectedInterests.filter((interest) => interest !== value);
        }
        setSelectedInterests(updatedInterests);
        // Save to LocalStorage
        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(updatedInterests));
        // Notify parent component
        onInterestsChange(updatedInterests);
    };

    return (
        <div className="interest-selector">
            <h3>What are you interested in?</h3>
            <div className="interest-options">
                {availableInterests.map((interest) => (
                    <label key={interest}>
                        <input
                            type="checkbox"
                            value={interest}
                            checked={selectedInterests.includes(interest)}
                            onChange={handleCheckboxChange}
                        />
                        {interest}
                    </label>
                ))}
            </div>
             <p style={{ fontSize: '0.8em', marginTop: '10px' }}>
                Selections saved in your browser's Local Storage.
            </p>
        </div>
    );
}

export default InterestSelector;