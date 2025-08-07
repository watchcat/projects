window.Telegram.WebApp.ready();

const fileList = document.getElementById('file-list');
const searchBar = document.getElementById('search-bar');

let allFiles = [];

function getToken() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('token');
}

async function fetchFiles() {
    const token = getToken();
    if (!token) {
        alert('Auth token not found.');
        return;
    }

    try {
        const response = await fetch('/api/files', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (response.ok) {
            const files = await response.json();
            allFiles = files;
            renderFiles(allFiles);
        } else {
            alert('Failed to fetch files. Check your token.');
        }
    } catch (error) {
        console.error('Error fetching files:', error);
        alert('An error occurred while fetching files.');
    }
}

function renderFiles(filesToRender) {
    fileList.innerHTML = '';
    filesToRender.forEach(file => {
        const li = document.createElement('li');
        const a = document.createElement('a');
        a.href = file.link;
        a.target = '_blank';
        a.textContent = file.name;

        const icon = document.createElement('span');
        icon.className = 'file-icon';
        if (file.name.endsWith('.mp3')) {
            icon.textContent = '🎵';
        } else {
            icon.textContent = '🎬';
        }

        li.appendChild(icon);
        li.appendChild(a);
        fileList.appendChild(li);
    });
}

searchBar.addEventListener('input', (e) => {
    const searchTerm = e.target.value.toLowerCase();
    const filteredFiles = allFiles.filter(file => file.name.toLowerCase().includes(searchTerm));
    renderFiles(filteredFiles);
});

fetchFiles();
