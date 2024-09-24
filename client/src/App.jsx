import "./App.css";
import { LoginPage } from "./components/LoginPage";
import { Profile } from "./components/Profile";

function App() {
    return (
        <div className="container">
            <h1>Welcome to the Home Page</h1>
            <LoginPage />
            <Profile />
        </div>
    );

}

export default App;
