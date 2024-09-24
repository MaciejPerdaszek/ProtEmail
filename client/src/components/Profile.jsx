import React, { useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import axios from "axios";

export function Profile () {
   const { user, getAccessTokenSilently, isAuthenticated} = useAuth0();

    // useEffect(() => {
    //     axios.get("http://localhost:8080/api/private")
    //         .then((response) => {
    //             console.log(response.data);
    //         })
    //         .catch((error) => {
    //             console.error(error);
    //         });
    // }, []);

  const fetchPrivateData = async () => {
    try {
      const token = await getAccessTokenSilently();
      console.log(token);
      const response = await axios.get("http://localhost:8080/api/private", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      console.log(response.data);
    } catch (error) {
      console.error(error);
    }
  };

  const home = () => {
    const response = axios.get("http://localhost:8080/api/")
    .then((response) => {
      console.log(response.data);
    })
  }


  return (
    <div>
      <button onClick={home}>Home</button>
      {isAuthenticated ? (
        <div>
          <h2>Welcome {user.name}!</h2>
          <button onClick={fetchPrivateData}>Fetch Private Data</button>
        </div>
      ) : (
        <h2>You are not logged in!</h2>
      )}
    </div>
  );
}