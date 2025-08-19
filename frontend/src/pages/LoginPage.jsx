import React from 'react';
import LoginForm from '../components/LoginForm'; // Import the form component
import styles from './LoginPage.module.css';

const LoginPage = () => {
    return (
        <div className={styles.pageContainer}>
        <LoginForm />
        </div>

    );
};

export default LoginPage;
