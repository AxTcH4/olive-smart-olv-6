## Presentation/Map layer: Drawing parcels
## Analysis Imagery: WorldView 

## Workflow:
``` 
1. PRESENTATION / MAP LAYER
   -> User draws a parcel (polygon).

2. IMAGERY LAYER
   -> Load the available WorldView-2 imagery covering that parcel.
   -> Check whether usable imagery exists for the required dates.

3. PARCEL VALIDATION
   -> Check whether the parcel contains olive trees.
   -> For the OQDS prototype, use its existing tree inventory.
   -> If no usable imagery or no relevant trees: stop and inform the user.

4. ANALYSIS CONFIGURATION
   -> User selects an analysis method.
      (Threshold / CUSUM / Isolation Forest / Siamese CNN / TCN)

5. DATA PREPROCESSING
   -> Retrieve the geographical coordinates of each tree.
   -> For each tree:
      -> Locate the corresponding pixels in the 2019 imagery.
      -> Locate the same geographical location in the 2021 imagery.
      -> Crop a 16x16 RGB patch from each date.
      -> Associate both patches with the same tree ID.

   -> Build an array of paired image patches.

6. ANALYSIS JOB
   -> Send the prepared data to the Python ML service.
   -> Run the selected model.
   -> Receive predictions and scores for each tree.

7. RESULTS
   -> Associate each prediction with its tree ID and coordinates.
   -> Store the results.
   -> Display them on the map and in parcel statistics.

```