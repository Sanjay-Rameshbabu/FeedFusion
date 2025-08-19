
import React from 'react';
// Corrected import name:
import SignupForm from '../components/SignupForm'; // Import the form component

const SignupPage = () => {
    return (
        <div className="signup-page-container flex items-center justify-center min-h-screen bg-gray-100">
            {/* You can add page-specific layout, headers, footers etc. here */}
            <SignupForm />
        </div>
    );
};

export default SignupPage;
