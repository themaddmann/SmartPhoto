# SmartPhoto
Camera Application that geotags photos with GPS metadata

There are five activities in the Smart Photo application:
•	MainActivity
o	This activity is displayed on launch and handles many of the necessary features of the application, such as obtaining the user's username, requesting the user activate location services, and, when the app is launched for the first time, requesting permission to utilize certain aspects of the phone (i.e. Read and Write access, etc.).
o	The activity contains three buttons and, potentially, multiple text fields. The buttons are Start New Trip, Delete Trip, and Exit, which are fairly self explanatory. The user is required to type out the full name of the trip they wish to delete. If there are previously existing trips, they will be displayed at the top of the screen and are clickable, which launches GalleryActivity.
•	CameraActivity
o	This activity is where the application allows the user to take pictures, automatically adding them to the current trip. It includes checks to make sure the user has not disabled locatoin services. It contains three buttons, an imageview, and a text field. The buttons are Open Camera, Delete Photo, and End Trip. Again, all of the buttons are fairly self-explanatory. Once the user has taken a picture, it will be displayed beneath the Open Camera button, with the relevant photo information displayed underneath it, such as filename, date and time, and GPS data.
•	GalleryActivity
o	This activity contains many of the important features of the application, and is launched by selecting one of the trips in MainActivity. It displays the name of the trip at the top of the screen and the number of photos inside the trip. At the bottom are five buttons, View Images, Continue Trip, View on Map, Upload Pictures, and Return. Once again, the names are fairly obvious: View Images launches ImageActivity; Continue Trip launches CameraActivity, but in "continue" mode so that any new images are added to the existing trip instead of to a new one; View on Map launches MapsActivity; Upload Pictures uploads all images in the trip to the server; and Return returns the app to MainActivity.
•	ImageActivity
o	This activity allows the user to view all of the images in a given trip. It displays the photos and the relevant photo information, such as filename, time and date, and GPS data. At the top is a Delete Photo button, which deletes the current photo being displayed and restarts the activity.
•	MapsActivity
o	This activity simply launches a GoogleMap and places pins at the location of all of the photos in the given trip. The user exits the activity by simply pressing the back button on the phone.
