import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext'; // Ensure path is correct
import { Link } from 'react-router-dom';
import { FiUser, FiMail, FiLock, FiLoader } from 'react-icons/fi'; // Import needed icons
import styles from './SignupForm.module.css';

const SignupForm = () => {
    const { signup, isLoading, error: authError } = useAuth();
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [localError, setLocalError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    useEffect(() => {
        if (authError) {
            setLocalError(authError);
            setSuccessMessage('');
        } else {        
            setLocalError('');
        }
    }, [authError]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLocalError('');
        setSuccessMessage('');

        if (password !== confirmPassword) {
            setLocalError('Passwords do not match.');
            return;
        }
        if (password.length < 6) {
            setLocalError('Password must be at least 6 characters long.');
            return;
        }

        const userData = { username, email, password };
        const success = await signup(userData);

        if (success) {
            setSuccessMessage('Account created successfully! Redirecting...');
            // Optional: Clear form fields after a delay or on successful navigation by context
        }
        // Error display is handled by the useEffect hook
    };

    return (
        // Apply styles using the imported 'styles' object
        <div className={styles.formContainer}>
            <div className={styles.header}>
                <h2 className={styles.title}>Create Account</h2>
                 <p className={styles.subtitle}>
                     Or{' '}
                     <Link to="/login" className={styles.link}>
                         log in to your existing account
                     </Link>
                 </p>
            </div>

            <form className={styles.form} onSubmit={handleSubmit}>
                {/* Display Error Message */}
                {localError && (
                    <div className={styles.errorMessage} role="alert">
                        <span>{localError}</span>
                    </div>
                )}
                {/* Display Success Message */}
                {successMessage && (
                     <div className={styles.successMessage} role="alert">
                        {/* Optional: Icon */}
                        {/* <FiCheckCircle className="inline w-5 h-5 mr-2"/> */}
                        <span >{successMessage}</span>
                     </div>
                )}

                {/* Input Fields Group */}
                <div className={styles.inputGroup}>
                    {/* Username Input */}
                    <div className={styles.inputWrapper}>
                        <div className={styles.inputIcon}>
                            <FiUser className={styles.icon} aria-hidden="true" />
                        </div>
                        <input
                            type="text"
                            id="signup-username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                            className={styles.inputField}
                            placeholder="Choose a username"
                            autoComplete="username"
                        />
                    </div>

                    {/* Email Input */}
                    <div className={styles.inputWrapper}>
                         <div className={styles.inputIcon}>
                            <FiMail className={styles.icon} aria-hidden="true" />
                        </div>
                        <input
                            type="email"
                            id="signup-email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            autoComplete="email"
                            className={styles.inputField}
                            placeholder="Enter your email"
                        />
                    </div>

                    {/* Password Input */}
                    <div className={styles.inputWrapper}>
                         <div className={styles.inputIcon}>
                            <FiLock className={styles.icon} aria-hidden="true" />
                        </div>
                        <input
                            type="password"
                            id="signup-password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            autoComplete="new-password"
                            className={styles.inputField}
                            placeholder="Create a password (min. 6 chars)"
                        />
                    </div>

                     {/* Confirm Password Input */}
                    <div className={styles.inputWrapper}>
                         <div className={styles.inputIcon}>
                            <FiLock className={styles.icon} aria-hidden="true" />
                        </div>
                        <input
                            type="password"
                            id="signup-confirm-password"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            required
                            autoComplete="new-password"
                            className={styles.inputField}
                            placeholder="Confirm your password"
                        />
                    </div>
                </div>


                {/* Submit Button */}
                <div>
                    <button
                        type="submit"
                        disabled={isLoading || !!successMessage}
                        className={styles.submitButton} // Reuse button style
                    >
                        {isLoading ? (
                             <FiLoader className={styles.buttonLoader} aria-hidden="true" />
                        ): (
                            'Create Account'
                        )}
                    </button>
                </div>

                 {/* Link to Login Page */}
                 <div className={styles.loginLinkContainer}>
                    <p className={styles.loginLinkText}>
                        Already have an account?{' '}
                        <Link to="/login" className={styles.link}>
                            Login
                        </Link>
                    </p>
                </div>
            </form>
        </div>
    );
};

export default SignupForm;

