/**
 * D3 Chord Diagram Renderer
 */

const width = window.innerWidth;
const height = window.innerHeight;
const outerRadius = Math.min(width, height) * 0.5 - 40;
const innerRadius = outerRadius - 30;

const formatValue = d3.formatPrefix(",.0", 1e3);

const chord = d3.chord()
    .padAngle(0.05)
    .sortSubgroups(d3.descending);

const arc = d3.arc()
    .innerRadius(innerRadius)
    .outerRadius(outerRadius);

const ribbon = d3.ribbon()
    .radius(innerRadius);

const color = d3.scaleOrdinal(d3.schemeCategory10);

const svg = d3.select("#chart").append("svg")
    .attr("width", width)
    .attr("height", height)
    .attr("viewBox", [-width / 2, -height / 2, width, height])
    .attr("style", "max-width: 100%; height: auto; font: 10px sans-serif;");

/**
 * Updates the diagram with new data.
 * @param {Object} data - Adjacency matrix and labels.
 * @param {Array<Array<number>>} data.matrix - Square Adjacency Matrix.
 * @param {Array<string>} data.names - Labels for each index.
 */
window.updateDiagram = function(data) {
    const { matrix, names } = data;

    svg.selectAll("*").remove();

    const chords = chord(matrix);

    const group = svg.append("g")
      .selectAll("g")
      .data(chords.groups)
      .join("g");

    group.append("path")
        .attr("fill", d => color(d.index))
        .attr("stroke", d => d3.rgb(color(d.index)).darker())
        .attr("d", arc);

    group.append("title")
        .text(d => `${names[d.index]}\n${formatValue(d.value)}`);

    const ticks = group.selectAll("g")
      .data(d => groupTicks(d, 1e3))
      .join("g")
        .attr("transform", d => `rotate(${d.angle * 180 / Math.PI - 90}) translate(${outerRadius},0)`);

    ticks.append("line")
        .attr("stroke", "currentColor")
        .attr("x2", 6);

    ticks.append("text")
        .attr("x", 8)
        .attr("dy", "0.35em")
        .attr("transform", d => d.angle > Math.PI ? "rotate(180) translate(-16)" : null)
        .attr("text-anchor", d => d.angle > Math.PI ? "end" : null)
        .text(d => formatValue(d.value));

    svg.append("g")
        .attr("fill-opacity", 0.67)
      .selectAll("path")
      .data(chords)
      .join("path")
        .attr("d", ribbon)
        .attr("fill", d => color(d.target.index))
        .attr("stroke", d => d3.rgb(color(d.target.index)).darker());
};

function groupTicks(d, step) {
  const k = (d.endAngle - d.startAngle) / d.value;
  return d3.range(0, d.value, step).map(value => {
    return {value: value, angle: value * k + d.startAngle};
  });
}

// Initial draw with sample data
const sampleData = {
    names: ["A", "B", "C", "D"],
    matrix: [
        [11975,  5871, 8916, 2868],
        [ 1951, 10048, 2060, 6171],
        [ 8010, 16145, 8090, 8045],
        [ 1013,   990,  940, 6907]
    ]
};
// window.updateDiagram(sampleData);
