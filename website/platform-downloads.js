(function () {
  var repo = 'https://api.github.com/repos/tetsuji16/ProjectLibre/releases?per_page=100&page=';
  var platforms = [
    { id: 'linux-x64', suffix: '-linux-x64.zip' },
    { id: 'macos-aarch64', suffix: '-macos-aarch64.zip' }
  ];

  function findLatestPackages(page, matches) {
    return fetch(repo + page, { headers: { Accept: 'application/vnd.github+json' } })
      .then(function (response) {
        if (!response.ok) throw new Error('Release lookup failed');
        return response.json();
      })
      .then(function (releases) {
        platforms.forEach(function (platform) {
          if (matches[platform.id]) return;
          var release = releases.find(function (item) {
            return !item.draft && !item.prerelease && item.assets.some(function (asset) {
              return asset.name.indexOf('microProject-') === 0 && asset.name.endsWith(platform.suffix);
            });
          });
          if (release) {
            matches[platform.id] = {
              release: release,
              asset: release.assets.find(function (asset) {
                return asset.name.indexOf('microProject-') === 0 && asset.name.endsWith(platform.suffix);
              })
            };
          }
        });
        if (platforms.every(function (platform) { return matches[platform.id]; }) || releases.length < 100) return matches;
        return findLatestPackages(page + 1, matches);
      });
  }

  findLatestPackages(1, {})
    .then(function (matches) {
      platforms.forEach(function (platform) {
        var match = matches[platform.id];
        var card = document.querySelector('[data-platform-download="' + platform.id + '"]');
        if (!card) return;
        var link = card.querySelector('a');
        var version = card.querySelector('[data-platform-version]');
        if (!match) {
          version.textContent = '安定版の配布物は未公開です';
          link.href = 'https://github.com/tetsuji16/ProjectLibre/releases';
          return;
        }
        link.href = match.asset.browser_download_url;
        version.textContent = match.release.tag_name;
      });
    })
    .catch(function () {
      document.querySelectorAll('[data-platform-version]').forEach(function (version) {
        version.textContent = '配布一覧で利用可能な版をご確認ください';
      });
      document.querySelectorAll('[data-platform-download] a').forEach(function (link) {
        link.href = 'https://github.com/tetsuji16/ProjectLibre/releases';
      });
    });
})();
