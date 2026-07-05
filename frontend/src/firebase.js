import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';

const firebaseConfig = {
  apiKey: "AIzaSyA7pMQnlMENDkZLnQsi1J7BczqAjmXj74Q",
  authDomain: "creatorengine-4eeed.firebaseapp.com",
  projectId: "creatorengine-4eeed",
  storageBucket: "creatorengine-4eeed.firebasestorage.app",
  messagingSenderId: "510915709770",
  appId: "1:510915709770:web:95af50141edb26bdbacfb3"
};


const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);