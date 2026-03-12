import { Card, CardContent, Stack, Typography } from '@mui/material';

interface SummaryCardProps {
  caption: string;
  eyebrow: string;
  tone: 'default' | 'accent' | 'warning';
  value: number | string;
}

function SummaryCard({ caption, eyebrow, tone, value }: SummaryCardProps) {
  return (
    <Card className={`summary-card summary-card--${tone}`} elevation={0}>
      <CardContent>
        <Typography className="summary-card__eyebrow" variant="overline">
          {eyebrow}
        </Typography>
        <Stack direction="row" alignItems="baseline" spacing={1}>
          <Typography variant="h2">{value}</Typography>
          <Typography color="text.secondary" variant="body2">
            {caption}
          </Typography>
        </Stack>
      </CardContent>
    </Card>
  );
}

export default SummaryCard;
