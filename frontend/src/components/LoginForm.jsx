// src/components/LoginForm.jsx
// Assuming you renamed Login.jsx to LoginForm.jsx as recommended
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiUser, FiLock, FiArrowRight, FiLoader, FiUserPlus } from 'react-icons/fi';
import { useAuth } from '../context/AuthContext'; // Ensure path is correct
// Import the CSS module file
import styles from './LoginForm.module.css';

// Component should be named LoginForm to match filename convention
export default function LoginForm() {
    const navigate = useNavigate();
    const { login, isLoading, error: authError } = useAuth();
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [localError, setLocalError] = useState(null);

    // Sync local error with error from AuthContext
    useEffect(() => {
        if (authError) {
            setLocalError(authError);
        } else {
            setLocalError(null);
        }
    }, [authError]);

    const handleLogin = async (e) => {
        e.preventDefault();
        setLocalError(null);
        const credentials = { username, password };
        await login(credentials);
        // Navigation and loading state are handled by context
    };

    // Navigate to the signup page
    const goToSignup = () => {
        navigate('/signup'); // Ensure this path matches your Route definition
    };

    return (
        // Apply styles using the imported 'styles' object
        // Removed the outer container that was previously styled with Tailwind
        // The page container styling is now handled by LoginPage.jsx and its CSS module
        <div className={styles.formContainer}> {/* Use the main container style from the module */}
            <div className={styles.header}>
                <h2 className={styles.title}>FeedFusion</h2>
                <p className={styles.subtitle}>Sign in to view your unified feed</p>
            </div>

            <form className={styles.form} onSubmit={handleLogin}>
                {localError && (
                    <div className={styles.errorMessage} role="alert">
                        <span>{localError}</span>
                    </div>
                )}

                {/* Removed inputGroup div as inputs are styled individually */}
                {/* Username Input */}
                <div className={styles.inputWrapper}>
                    <div className={styles.inputIcon}>
                        <FiUser className={styles.icon} aria-hidden="true" />
                    </div>
                    <input
                        type="text"
                        name="username"
                        id="login-username"
                        required
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className={styles.inputField}
                        placeholder="Username or Email"
                        autoComplete="username"
                    />
                </div>

                {/* Password Input */}
                <div className={styles.inputWrapper}>
                    <div className={styles.inputIcon}>
                        <FiLock className={styles.icon} aria-hidden="true" />
                    </div>
                    <input
                        type="password"
                        name="password"
                        id="login-password"
                        required
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className={styles.inputField}
                        placeholder="Password"
                        autoComplete="current-password"
                    />
                </div>


                {/* Submit Button */}
                <div>
                    <button
                        type="submit"
                        disabled={isLoading}
                        className={styles.submitButton}
                    >
                        {isLoading ? (
                            <FiLoader className={styles.buttonLoader} aria-hidden="true" />
                        ) : (
                            <>
                                Sign in
                                <FiArrowRight className={styles.buttonIcon} aria-hidden="true" />
                            </>
                        )}
                    </button>
                </div>

                {/* Separator */}
                <div className={styles.separator}>
                    <div className={styles.separatorLine} aria-hidden="true">
                        <div className={styles.line}></div>
                    </div>
                    <div className={styles.separatorTextContainer}>
                        <span className={styles.separatorText}>New to FeedFusion?</span>
                    </div>
                </div>

                {/* Create Account Button */}
                <div>
                    <button
                        type="button"
                        onClick={goToSignup} // Use the dedicated handler
                        className={styles.secondaryButton}
                    >
                        <FiUserPlus className={styles.secondaryButtonIcon} aria-hidden="true" />
                        Create an Account
                    </button>
                </div>
            </form>
        </div>
    );
}
