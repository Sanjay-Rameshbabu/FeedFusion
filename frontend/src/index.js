// src/index.js
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom'; // Import BrowserRouter
import './index.css'; // Global styles
import App from './App';
import { AuthProvider } from './context/AuthContext'; // Import AuthProvider
import reportWebVitals from './reportWebVitals';
import { setAuthToken } from './services/api'; // Import function to set token on initial load

// Check for existing token in localStorage and set it for API calls on initial load
const token = localStorage.getItem('authToken');
if (token) {
  console.log("Token found in storage on initial load.");
  setAuthToken(token);
} else {
  console.log("No token found in storage on initial load.");
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    {/* BrowserRouter provides routing context */}
    <BrowserRouter>
       {/* AuthProvider provides authentication context */}
       {/* Place AuthProvider inside BrowserRouter if it uses hooks like useNavigate */}
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
