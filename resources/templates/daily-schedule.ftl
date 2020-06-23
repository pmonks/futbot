[#ftl output_format="HTML" auto_esc=true strip_whitespace=true]
<!DOCTYPE html>
<html>
  <head>
    <link href="data:image/x-icon;base64,AAABAAEAEBAAAAEAIABoBAAAFgAAACgAAAAQAAAAIAAAAAEAIAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQEBGN/f33YAAAC5AAAA2gAAANoAAAC539/fdgICAhgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABzs7Oc/////T/////AAAA/wAAAP8AAAD/AAAA///////////0zs7OcwAAAAEAAAAAAAAAAAAAAAAAAAAB9fX1nf/////////////////////////////////////////////////////29vadAAAAAQAAAAAAAAAAqqqqc////////////////////////////////////////////////////////////////7CwsHMAAAAAAAAAGAAAAPRZWVn//////////////////////////////////////////////////////2dnZ/8AAAD0AAAAGAAAAHYAAAD/Wlpa//////////////////////8AAAD/AAAA//////////////////////9nZ2f/AAAA/wAAAHYAAAC5AAAA/5iYmP///////////8HBwf8AAAD/AAAA/wAAAP8AAAD/uLi4////////////o6Oj/wAAAP8AAAC5PDw82igoKP////////////////8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP////////////////80NDT/JiYm2v///9r/////////////////////b29v/wAAAP8AAAD/AAAA/wAAAP9jY2P//////////////////////////9r///+5//////////////////////////8AAAD/AAAA/wAAAP8AAAD///////////////////////////////+51dXVdv//////////////////////////Tk5O/1paWv9aWlr/UVFR////////////////////////////1tbWdgAAABj////0////////////////////////////////////////////////////////////////////9AAAABgAAAAAtra2dBoaGv8AAAD/gYGB/////////////////////////////////4KCgv8AAAD/ERER/7a2tnQAAAAAAAAAAAAAAAEAAACeAAAA/wAAAP/Pz8///////////////////////9jY2P8AAAD/AAAA/wAAAJ4AAAABAAAAAAAAAAAAAAAAAAAAAQAAAHQAAAD0AAAA//////////////////////8CAgL/AAAA9AAAAHQAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGLu7u3b///+5////2v///9r///+5vb29dgAAABgAAAAAAAAAAAAAAAAAAAAA8A8AAMADAACAAQAAgAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAEAAIABAADAAwAA8A8AAA==" rel="icon" type="image/x-icon">
    <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Roboto:300,300italic,700,700italic">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/normalize/8.0.1/normalize.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/milligram/1.4.0/milligram.css">
    <title>Soccer Matches - ${day}</title>
  </head>
  <body>
    <h1>Matches scheduled for today (${day}):</h1>
    <p>
    [#if matches?? && matches?size > 0]
      <table>
        <thead><tr><th>Competition</th><th>Stage</th><th>Home Team</th><th>Away Team</th></tr></thead>
        <tbody>
      [#assign last_date_time = ""]
      [#list matches as match]
        [#if match.utc_date != last_date_time]
          [#assign last_date_time = match.utc_date!""]
          [#assign match_time = match.utc_date?datetime.xs?time]
          <tr><td colspan="4"><b>Scheduled start <a target="_blank" href="https://www.thetimezoneconverter.com/?t=${match_time?url}&tz=UTC">${match_time} UTC</a>:</b></td></tr>
        [/#if]
          <tr>
            <td>[#if match.competition.area.ensign_url??]<img src="${match.competition.area.ensign_url}" height="12px"/> [/#if]${match.competition.name!"Unknown"}</td>
            <td>${match.group!"Unknown"}</td>
            <td>${match.home_team.name!"Unknown"}</td>
            <td>${match.away_team.name!"Unknown"}</td>
          </tr>
      [/#list]
        </tbody>
      </table>
    [#else]
      Sadly there are no ⚽️ matches scheduled for today. 😢
    [/#if]
    </p>
  </body>
</html>
