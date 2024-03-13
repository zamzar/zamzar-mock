#!/bin/bash

# Function to fetch and process a single page of API results
# $1 - The 'after' parameter for pagination
fetch_and_process_page() {
    local after=$1
    local url="https://api.zamzar.com/v1/formats/"

    # Append the 'after' parameter to the URL if provided
    if [ -n "$after" ]; then
        url="${url}?after=$after"
    fi

    # Fetch the API response and process it in memory
    response=$(curl -u "${ZAMZAR_API_KEY}:" -s "$url")

    # Use jq to check if the data array is empty
    data_empty=$(echo "$response" | jq -e '.data | length == 0')

    # If data is empty, we're done
    if [ "$data_empty" = "true" ]; then
        return
    fi

    # Extract format names and the 'last' value for pagination
    names=$(echo "$response" | jq -r '.data[].name')
    last=$(echo "$response" | jq -r '.data[-1].name')

    for name in $names; do
        # For each format name, extract the corresponding JSON object and save it to a file
        echo "$response" | jq -r --arg NAME "$name" '.data[] | select(.name==$NAME)' > "${output_dir}/${name}.json"
    done

    # Recursively fetch the next page
    fetch_and_process_page "$last"
}

# Use the first script argument as the output directory, default to current working directory
output_dir="${1:-.}"

# Ensure the output directory exists
if [ ! -d "$output_dir" ]; then
    echo "The specified output directory does not exist: $output_dir"
    exit 1
fi

# Check if the ZAMZAR_API_KEY environment variable is set
if [ -z "${ZAMZAR_API_KEY}" ]; then
    echo "Error: ZAMZAR_API_KEY environment variable is not set"
    exit 1
fi

# Start processing from the first page
fetch_and_process_page


